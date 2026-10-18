const pool = require("../database/maria");
const express = require("express");
const { translate } = require("../services/gemini");
const AuthMiddleWare = require("../middleware/auth");

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

router
.post("/", AuthMiddleWare, async (req, res) => {
    const { profileId, text, context, speaker } = req.body;

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
        const preprocessed = replaceCharacterNames(text, characters);
        console.log(`[전처리] ${text.replace(/\n/g, " / ")} → ${preprocessed.replace(/\n/g, " / ")}`);

        // 화자(optional): 등록된 캐릭터면 한국어 이름으로 전달
        let speakerName = null;
        if (speaker) {
            const found = characters.find(c => c.source_name === speaker);
            speakerName = found ? found.target_name : null;
        }

        const translatedText = await translate({
            text: preprocessed,
            sourceLanguage: profile[0].sourcelanguage,
            glossary: words,
            context: context || [],
            speaker: speakerName
        });

        return res.status(200).json({
            message: "번역에 성공했습니다.",
            originalText: text,
            translatedText
        });

    } catch (err) {
        console.error(`번역 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message: "번역에 실패했습니다."
        });
    }
});

module.exports = router;