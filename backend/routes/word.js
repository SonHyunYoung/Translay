const AuthMiddleWare = require("../middleware/auth"); //로그인 인증

const pool = require("../database/maria"); //db 연결

//router 관련 모듈
const express = require("express");
const router = express.Router();

//고유 명사 CURD
router
.get("/:profileId", AuthMiddleWare, async(req, res) => { //등록한 고유 명사 조회
    const {profileId} = req.params;

    try{
        // 프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                            FROM profiletbl
                            WHERE profile_id = ? AND uid = ?`;

        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);

        if(profile.length === 0){
            return res.status(403).json({
                message : "접근 권한이 없습니다."
            });
        }

        //등록 명사 조회
        const getSql = `SELECT word_id, source, target
                      WHERE profile_id = ?`;

        const [word] = await pool.query(getSql, [profileId]);

        return res.status(200).json({
            message : "단어 조회를 성공했습니다.",
            word
        });

    } catch(err) {
        console.error(`단어 조회 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "단어 조회 중 오류 발생"
        });
    }
})  
.post("/profileId", AuthMiddleWare, async(req, res) => { //고유명사 등록
    //저장할 소스
    const {profileId} = req.params;
    const {source, target} = req.body;

    if(!source || !target){
        return res.status(400).json({
            message : "필수 입력사항을 모두 입력해주세요."
        });
    }

    try{
        // 프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                            FROM profiletbl
                            WHERE profile_id = ? AND uid = ?`;

        const [profile] = await pool.query(checkSql, [profiledId, req.user.id]);

        if(profile.length === 0){
            return res.status(403).json({
                message : "접근 권한이 없습니다."
            });
        }

        const insertSql = `INSERT INTO wordtbl (profile_id, source, target)
                            VALUES (?, ?, ?)`;

        await pool.query(insertSql, [profileId, source, target]);

        return res.status(201).json({
            message : "단어를 성공적으로 저장했습니다."
        }); 

    } catch(err) {
        console.error(`단어 등록 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "단어 등록 중 오류가 발생했습니다."
        });
    }
})
.patch("/:profileId/:wordId", AuthMiddleWare, async(req, res) => { //고유명사 수정
    const {profileId, wordId} = req.params;
    const {source, target} = req.body;
    
    try{
        // 프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                            WHERE profile_id = ? AND uid = ?`;

        const [profile] = await pool.query(checkSql, [profiledId, req.user.id]);

        if(profile.length === 0){
            return res.status(403).json({
                message : "접근 권한이 없습니다."
            });
        }

        //수정 
        const updateSql = `UPDATE SET 
                            source = ?,
                            traget = ?,
                            updated_at = NOW()
                            WHERE profile_id = ? AND word_id = ?`;

        await pool.query(updateSql, [source, target, profileId, wordId]);

        return res.status(200).json({
            message : "단어를 성공적으로 수정했습니다."
        });

    } catch(err) {
        console.error(`단어 수정 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "단어 수정 중 오류가 발생했습니다."
        })
    }
})
.delete("/:profileId/:wordId", AuthMiddleWare, async(req, res) => { //고유명사 삭제
    const {profileId, wordId} = req.params;

    try{
        // 프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                            FROM profiletbl
                            WHERE profile_id = ? AND uid = ?`;

        const [profile] = await pool.query(checkSql, [profiledId, req.user.id]);

        if(profile.length === 0){
            return res.status(403).json({
                message : "접근 권한이 없습니다."
            });
        }

        //삭제
        const deleteSql = `DELETE FROM wordtbl 
                            WHERE profile_id = ? AND word_id = ?`;

        await pool.query(deleteSql, [profileId, wordId]);

        return res.status(200).json({
            message : "단어 삭제를 성공했습니다."
        });

    } catch(err) {
        console.error(`단어 삭제 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "단어 삭제 중 오류가 발생했습니다."
        });
    }
});

module.exports = router;