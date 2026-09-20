const pool = require("../database/maria");

const express = require("express");
const router = express.Router();

const { translate } = require('../services/gemini');

router
.post("/", async(req, res) => {
    const { profileId, text, sourceLanguage, context } = req.body;

    try {
        // 프로필 소유권 검증
        const profile = await pool.query(
        'SELECT * FROM game_profiles WHERE profile_id = ? AND user_id = ?',
        [profileId, req.user.userId]
        );
        if (!profile.length) return res.status(403).json({ success: false, error: { code: 'FORBIDDEN' } });

        // 캐릭터 + 캐릭터 용어 조회
        const characters = await pool.query(
        `SELECT c.*, 
            JSON_ARRAYAGG(JSON_OBJECT('source', ct.source, 'target', ct.target, 'description', ct.description)) as terms
        FROM characters c
        LEFT JOIN character_terms ct ON c.character_id = ct.character_id
        WHERE c.profile_id = ?
        GROUP BY c.character_id`,
        [profileId]
        );

        // 단어(지명 등) 조회
        const glossary = await pool.query(
        'SELECT source, target FROM words WHERE profile_id = ?',
        [profileId]
        );

        // Gemini 호출
        const translatedText = await translate({
        text,
        sourceLanguage: profile[0].source_language,
        glossary,
        characters,
        context: context || [],
        });

        res.status(200).json({ 
            success: true, 
            data: { originalText: text, translatedText } });

    } catch (err) {
        console.error(`번역 api 호출 중 오류 발생 : ${err}`);

        res.status(500).json({ 
            success: false, 
            error: { code: 'GEMINI_API_ERROR', message: err.message } });
    }
});

module.exports = router;