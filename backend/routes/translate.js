const pool = require("../database/maria");
const express = require("express");
const { translate } = require("../services/gemini");
const AuthMiddleWare = require("../middleware/auth");

const router = express.Router();

router
.post("/", AuthMiddleWare, async (req, res) => {
    const { profileId, text, context } = req.body;

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
        const [characters] = await pool.query(
            `SELECT c.character_id, c.source_name, c.target_name,
                JSON_ARRAYAGG(
                    JSON_OBJECT('source', ct.source, 'target', ct.target, 'description', ct.description)
                ) as terms
             FROM charactertbl c
             LEFT JOIN charactertermtbl ct ON c.character_id = ct.character_id
             WHERE c.profile_id = ?
             GROUP BY c.character_id`,
            [profileId]
        );

        // 캐릭터 이외의 고유명사 조회
        const [words] = await pool.query(
            "SELECT source, target FROM wordtbl WHERE profile_id = ?",
            [profileId]
        );

        // Gemini API 호출
        const translatedText = await translate({
            text,
            sourceLanguage: profile[0].sourcelanguage,
            characters,
            glossary: words,
            context: context || []
        });

        return res.status(200).json({
            message: "번역에 성공했습니다.",
            originalText: text,
            translatedText
        });

    } catch (err) {
        console.error(`번역 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message: "번역 중 오류가 발생했습니다."
        });
    }
});

module.exports = router;