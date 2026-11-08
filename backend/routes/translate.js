const pool = require("../database/maria");
const express = require("express");
const { translate, resolveSpeaker } = require("../services/gemini");
const AuthMiddleWare = require("../middleware/auth");
const ProfileOwner = require("../middleware/profileOwner");

const router = express.Router();

// 호칭이 없을 때 JSON_ARRAYAGG가 만드는 null 객체 제거 + 문자열로 오는 경우 파싱
function normalizeCharacters(rows) {
    return rows.map(c => {
        let terms = c.terms;
        if (typeof terms === "string") {
            try { terms = JSON.parse(terms); } catch { terms = []; }
        }
        terms = (terms || []).filter(t => t && t.source && t.target);
        return { ...c, terms };
    });
}

// 원문의 호칭을 등록된 한국어로 미리 치환 (긴 것부터)
// - 호칭은 "A가 남을 부르는 말"이라 현재 화자(speakerChar)에게 연결된 것만 치환한다.
// - 캐릭터 이름(source_name)은 치환하지 않는다. 한 글자 한자 이름(光 등)이
//   일반 단어와 겹쳐서 엉뚱하게 바뀌는 것을 막기 위해, 번역 프롬프트에 힌트로만 넘긴다.
function replaceCharacterNames(text, characters, speakerChar) {
    if (!speakerChar) return text;

    const pairs = speakerChar.terms
        .filter(t => t.source && t.target)
        .map(t => ({ from: t.source, to: t.target }));

    pairs.sort((a, b) => b.from.length - a.from.length);

    let result = text;
    for (const p of pairs) {
        result = result.split(p.from).join(p.to);
    }
    return result;
}

function levenshtein(a, b) {
    const dp = Array.from({ length: a.length + 1 }, (_, i) => [i]);
    for (let j = 1; j <= b.length; j++) dp[0][j] = j;
    for (let i = 1; i <= a.length; i++) {
        for (let j = 1; j <= b.length; j++) {
            dp[i][j] = a[i - 1] === b[j - 1]
                ? dp[i - 1][j - 1]
                : Math.min(dp[i - 1][j - 1], dp[i - 1][j], dp[i][j - 1]) + 1;
        }
    }
    return dp[a.length][b.length];
}

// 가로선 모양 글자(一 ー — ― － ｰ - – − ─ 등)를 하나로 통일 (비교용)
const DASH_LIKE = /[ー一—―－ｰ\-‐‑‒–−─]/g;
const fold = (s) => s.replace(DASH_LIKE, "一");

// OCR로 읽은 이름표를 등록된 캐릭터 중 가장 가까운 것에 매칭 (모호하면 null)
function matchSpeaker(speaker, characters) {
    const s = fold(speaker.replace(/\s+/g, ""));
    if (!s) return null;

    const names = characters
        .filter(c => c.source_name)
        .map(c => ({ c, n: fold(c.source_name.replace(/\s+/g, "")) }));

    // 1) 정확 일치 (한 글자 이름 포함)
    const exact = names.filter(x => x.n === s);
    if (exact.length === 1) return exact[0].c;

    // 2) 포함 매칭: OCR 값 안에 등록 이름이 들어 있으면 (한 글자 이름은 오탐이 커서 제외)
    const contained = names
        .filter(x => x.n.length >= 2 && s.includes(x.n))
        .sort((a, b) => b.n.length - a.n.length);
    if (contained.length > 0) return contained[0].c;

    // 3) 레벤슈타인 (동률이면 null)
    let best = null, bestDist = Infinity, tie = false;
    for (const x of names) {
        const d = levenshtein(s, x.n);
        if (d < bestDist) { best = x; bestDist = d; tie = false; }
        else if (d === bestDist) { tie = true; }
    }
    if (!best || tie) return null;

    const len = best.n.length;
    const limit = len <= 1 ? 0 : len === 2 ? 1 : len === 3 ? 1 : 2;
    return bestDist <= limit ? best.c : null;
}

// 화자 확정: 로컬 매칭 우선, 실패 시 LLM 이름 추론(B안)
async function resolveSpeakerChar(speaker, characters) {
    if (!speaker || !speaker.trim()) {
        console.log("[화자] OCR 이름 없음(빈 값) → 이름 없이 진행");
        return null;
    }

    const normalized = fold(speaker.replace(/\s+/g, ""));
    console.log(`[화자] OCR 원본='${speaker}' / 정규화='${normalized}'`);

    // 1) 로컬 매칭 (정확/포함/편집거리) — 대부분 여기서 끝, 추가 호출 없음
    const local = matchSpeaker(speaker, characters);
    if (local) {
        console.log(`[화자] 로컬 매칭 성공 → ${local.source_name} (${local.target_name})`);
        return local;
    }

    // 영문자·숫자가 섞인 값은 일본어 이름이 아니라 OCR 오인식이므로 LLM에 묻지 않는다
    //  (엉뚱한 인물로 억지 매칭되는 것을 방지)
    if (/[A-Za-z0-9]/.test(normalized)) {
        console.log(`[화자] 영문/숫자가 섞인 값('${normalized}') → 오인식으로 보고 이름 없이 진행`);
        return null;
    }

    // 후보별 편집거리 (원인 파악용)
    const dists = characters
        .filter(c => c.source_name)
        .map(c => ({ name: c.source_name, d: levenshtein(normalized, fold(c.source_name.replace(/\s+/g, ""))) }))
        .sort((a, b) => a.d - b.d)
        .slice(0, 5)
        .map(x => `${x.name}(${x.d})`)
        .join(", ");
    console.log(`[화자] 로컬 매칭 실패. 가까운 후보(편집거리): ${dists}`);

    // 글자 모양은 같아 보여도 코드값이 다르면 매칭이 안 된다 → 비교용으로 코드값 출력
    const cp = (str) => [...str].map(ch => "U+" + ch.codePointAt(0).toString(16).toUpperCase().padStart(4, "0")).join(" ");
    const closest = characters
        .filter(c => c.source_name)
        .map(c => ({ n: c.source_name, d: levenshtein(normalized, fold(c.source_name.replace(/\s+/g, ""))) }))
        .sort((a, b) => a.d - b.d)[0];
    console.log(`[화자] 코드값 비교: OCR '${normalized}'=${cp(normalized)}` +
        (closest ? ` / 가장 가까운 등록 '${closest.n}'=${cp(closest.n)}` : ""));

    // 2) 실패 시에만 LLM에게 물어봄. 등록된 인물 전체(원문 + 한국어 이름)를 후보로 준다.
    //    한 글자 이름끼리는 편집거리가 전부 동점이라 상위 N명 자르기는 부정확하므로,
    //    가까운 순으로 정렬만 하고 최대 40명까지 넘긴다.
    const candidates = characters
        .filter(c => c.source_name)
        .map(c => ({
            c,
            d: levenshtein(normalized, fold(c.source_name.replace(/\s+/g, "")))
        }))
        .sort((a, b) => a.d - b.d)
        .slice(0, 40)
        .map(x => ({ source: x.c.source_name, target: x.c.target_name || "" }));
    if (candidates.length === 0) return null;

    const picked = await resolveSpeaker({ ocrName: speaker, candidates });
    if (!picked) {
        console.log("[화자] LLM 추론도 실패 → 이름 없이 진행");
        return null;
    }

    const found = characters.find(c => c.source_name === picked.source) || null;
    console.log(`[화자] LLM 추론 성공 → ${picked.source} (${picked.target})`);
    return found;
}

router
.post("/", AuthMiddleWare, async (req, res) => {
    const { profileId, text, speaker } = req.body;

    if (!profileId || !text) {
        return res.status(400).json({
            message: "필수 입력사항을 모두 입력해주세요."
        });
    }

    try {
        // 프로필 소유 검증
        const [profile] = await pool.query(
            "SELECT * FROM profiletbl WHERE profile_id = ? AND uid = ?",
            [profileId, req.user.id]
        );

        if (profile.length === 0) {
            return res.status(403).json({
                message: "접근 권한이 없습니다."
            });
        }

        // 캐릭터 + 호칭 조회
        const [characterRows] = await pool.query(
            `SELECT c.character_id, c.source_name, c.target_name,
                JSON_ARRAYAGG(
                    JSON_OBJECT('source', ct.source, 'target', ct.target)
                ) as terms
             FROM charactertbl c
             LEFT JOIN charactertermtbl ct ON c.character_id = ct.character_id
             WHERE c.profile_id = ?
             GROUP BY c.character_id`,
            [profileId]
        );
        const characters = normalizeCharacters(characterRows);

        // 캐릭터 이외의 고유명사 조회
        const [words] = await pool.query(
            "SELECT source, target FROM wordtbl WHERE profile_id = ?",
            [profileId]
        );

        // 화자 확정을 치환보다 먼저: 호칭 치환이 화자에 의존하므로
        const speakerChar = await resolveSpeakerChar(speaker, characters);
        const speakerName = speakerChar ? speakerChar.target_name : null;
        console.log(`[화자] 최종 결과: ${speakerName || "없음"} (호칭 ${speakerChar ? speakerChar.terms.length : 0}개 적용)`);

        // 전처리 치환
        // 줄바꿈 정리: 일본어/중국어는 붙이고, 영어는 공백으로 연결
        const joiner = profile[0].sourcelanguage === "en" ? " " : "";
        const cleaned = text.replace(/\s*\r?\n\s*/g, joiner).trim();

        const preprocessed = replaceCharacterNames(cleaned, characters, speakerChar);
        console.log(`[전처리] ${cleaned} → ${preprocessed}`);

        const translatedText = await translate({
            text: preprocessed,
            sourceLanguage: profile[0].sourcelanguage,
            glossary: words,
            speaker: speakerName,
            nameHints: characters
                .filter(c => c.source_name && c.target_name)
                .map(c => ({ source: c.source_name, target: c.target_name }))
        });

        return res.status(200).json({
            message: "번역에 성공했습니다.",
            originalText: text,
            translatedText,
            speakerName
        });

    } catch (err) {
        console.error(`번역 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message: "번역에 실패했습니다."
        });
    }
});

module.exports = router;