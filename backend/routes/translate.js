const pool = require("../database/maria");
const express = require("express");
const { translate } = require("../services/gemini");
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

// 원문의 캐릭터 이름/호칭을 등록된 한국어로 미리 치환 (긴 것부터)
function replaceCharacterNames(text, characters) {
    const pairs = [];
    for (const c of characters) {
        if (c.source_name && c.target_name) {
            pairs.push({ from: c.source_name, to: c.target_name });
        }
        for (const t of c.terms) {
            pairs.push({ from: t.source, to: t.target });
        }
    }
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

// 가로선 모양 글자(一 ー — ― － ｰ)를 하나로 통일 (비교용)
const DASH_LIKE = /[ー一—―－ｰ]/g;
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

        // 전처리 치환
        // 줄바꿈 정리: 일본어/중국어는 붙이고, 영어는 공백으로 연결
        const joiner = profile[0].sourcelanguage === "en" ? " " : "";
        const cleaned = text.replace(/\s*\r?\n\s*/g, joiner).trim();

        const preprocessed = replaceCharacterNames(cleaned, characters);
        console.log(`[전처리] ${cleaned} → ${preprocessed}`);
        

        // 화자: 가장 가까운 등록 캐릭터의 번역명 (실패 시 null)
        let speakerName = null;
        if (speaker) {
            const found = matchSpeaker(speaker, characters);
            speakerName = found ? found.target_name : null;
            console.log(`[화자] ${speaker} → ${speakerName}`);
        }

        const translatedText = await translate({
            text: preprocessed,
            sourceLanguage: profile[0].sourcelanguage,
            glossary: words,
            speaker: speakerName
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