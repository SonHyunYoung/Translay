const AuthMiddleWare = require("../middleware/auth"); //로그인 검증
 
const pool = require("../database/maria"); //db연결

const express = require("express"); //라우터 모듈
const router = express.Router(); //라우터

//캐릭터 이름 등록
router
.get("/:profileId", AuthMiddleWare, async(req, res) => {
    const {profileId} = req.params;

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }
        
        //조회 sql
        const getSql = `SELECT character_id, source_name, target_name
                        FROM charactertbl
                        WHERE profile_id = ?`;

        const [character] = await pool.query(getSql, [profileId]);

        return res.status(200).json({
            message : "캐릭터 이름 조회에 성공했습니다.",
            character
        });

    } catch (err) {
        console.error(`캐릭터 이름 조회 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : `캐릭터 이름 조회 중 오류가 발생했습니다.`
        });
    }
})
.post("/:profileId", AuthMiddleWare, async(req, res) => {
    const {profileId} = req.params;
    const {source, target, terms} = req.body;

    //필수사항 검증
    if(!target || !source) {
        return res.status(400).json({
            message : "필수사항을 모두 입력해주세요."
        });
    }

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        //중복 체크(동명이인)
        const dupliCheckSql = `SELECT profile_id, source_name 
                                FROM charactertbl
                                WHERE profile_id = ? AND source_name = ?`;

        const [exist] = await pool.query(dupliCheckSql, [profileId, source]);

        if(exist.length > 0){
            return res.status(400).json({
                message : "이미 등록되어 있는 캐릭터 입니다."
            });
        }

        //삽입
        const insertSql = `INSERT INTO charactertbl (profile_id, source_name, target_name)
                            VALUES (?, ?, ?)`;

        const [result] = await pool.query(insertSql, [profileId, source, target]);

        //호칭 저장(선택적 사항, 입력이 있을때만 등록)
        if(terms && Array.isArray(terms) && terms.length > 0){
            const termInsertSql = `INSERT INTO charactertermtbl (character_id, source, target)
                                    VALUES (?, ?, ?)`;  

            for(const term of terms){
                await pool.query(termInsertSql, 
                    [result.insertId, term.source, term.target]);
            }
        }

        return res.status(201).json({
            message : "캐릭터 정보를 성공적으로 등록했습니다."
        });

    } catch(err) {
        console.error(`캐릭터 이름 등록 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "캐릭터 이름 등록 중 오류가 발생했습니다."
        });
    }
})
.patch("/:profileId/:characterId", AuthMiddleWare, async(req, res) => { //캐릭터 수정
    const {profileId, characterId} = req.params;
    const {source, target} = req.body;

    if(!source || !target){
        return res.status(400).json({
            message : "필수사항을 모두 입력해주세요."
        });
    }

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        const updateSql = `UPDATE charactertbl
                            SET source_name = ?, target_name = ?, updated_at = NOW()
                            WHERE character_id = ? AND profile_id = ?`;

        await pool.query(updateSql, [profileId, characterId]);

        return res.status(200).json({
            message : "캐릭터 정보를 성공적으로 수정했습니다."
        });

    } catch(err) {
        console.error(`캐릭터 정보 수정 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "캐릭터 정보 수정 중 오류가 발생했습니다."
        });
    }

})
.delete("/:profileId/:characterId", AuthMiddleWare, async(req, res) => {
    const {profileId, characterId} = req.params;

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        const deleteId = `DELETE FROM charactertbl
                            WHERE character_id = ? AND profile_id = ?`

        await pool.query(deleteId, [characterId, profileId]);

        return res.status(200).json({
            message : "캐릭터 정보를 성공적으로 삭제했습니다."
        });

    } catch(err){
        console.error(`캐릭터 정보 삭제 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "캐릭터 정보 삭제 중 오류가 발생했습니다."
        });
    }
});

//캐릭터 호칭 등록
router
.get("/:profileId/:characterId/term", AuthMiddleWare, async(req, res) => { //호칭 조회
    const {profileId, characterId} = req.params;
    
    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        //조회
        const getSql = `SELECT term_id, source, target
                        FROM charactertermtbl
                        WHERE character_id = ?`;

        const [terms] = await pool.query(getSql, [characterId]);

        return res.status(200).json({
            message : "호칭 종회에 성공했습니다.",
            terms
        });

    } catch(err) {
        console.error(`호칭 조회 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "호칭 조회 중 오류가 발생했습니다."
        });
    }
})
.post("/:profileId/:characterId/term", AuthMiddleWare, async(req, res) =>{ //호칭 등록
    const {profileId, characterId} = req.params;
    const {source, target} = req.body;

    if(!source || !target){
        return res.status(400).json({
            message : "필수사항을 모두 입력해주세요"
        });
    }

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        //중복체크
        const dupliCheckSql = `SELECT * FROM charactertermtbl 
                                WHERE character_id = ? AND source = ?`;
        
        const [check] = await pool.query(dupliCheckSql, [characterId, source]);

        if(check.length > 0) {
            return res.status(400).json({
                message : "동일한 호칭이 존재합니다."
            });
        }
        
        //삽입
        const insertSql = `INSERT INTO charactertermtbl (character_id, source, target)
                            VALUES (?, ?, ?)`;
        
        await pool.query(insertSql, [characterId, source, target]);

        return res.status(201).json({
            message : "호칭을 성공적으로 등록했습니다."
        });

    } catch (err) {
        console.error(`호칭 등록 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "호칭 등록 중 오류가 발생했습니다."
        });
    }
})
.patch("/:profileId/:characterId/terms/:termId", AuthMiddleWare, async(req, res) => { //호칭 수정
    const {profileId, characterId, termId} = req.params;
    const {source, target} = req.body;

    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        //수정
        const updateSql = `UPDATE charactertermtbl
                            SET source = ?, target = ?, updated_at = NOW()
                            WHERE term_id = ? AND character_id = ?`;
        
        await pool.query(updateSql, [source, target, termId, characterId]);

        return res.status(200).json({
            message : "호칭을 성공적으로 수정했습니다."
        });

    } catch (err) {
        console.error(`호칭 수정 종 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "호칭 수정 중 오류가 발생했습니다."
        });
    }
})
.delete("/:profileId/:characterId/terms/:termId", AuthMiddleWare, async(req, res) => { //호칭 삭제
    const {profileId, characterId, termId} = req.params;
    
    try{
        //프로필 검증
        const checkSql = `SELECT profile_id, uid, name, sourcelanguage
                                    FROM profiletbl
                                    WHERE profile_id = ? AND uid = ?`;
        
        const [profile] = await pool.query(checkSql, [profileId, req.user.id]);
        
        if(profile.length === 0){
            return res.status(403).json({
                 message : "접근 권한이 없습니다."
            });
        }

        //삭제
        const deleteSql = `DELETE 
                            FROM charactertermtbl
                            WHERE term_id = ? AND character_id = ?
                        `
        await pool.query(deleteSql, [termId, characterId]);

        return res.status(200).json({
            message : "호칭을 성공적으로 삭제했습니다."
        });

    } catch (err) {
        console.error(`호칭 삭제 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "호칭 삭제 중 오류가 발생했습니다."
        });
    }
});

module.exports = router;