//프로필 관련 CRUD

const pool = require("../database/maria"); //db 연결 

const express = require("express"); //express 모듈
const router = express.Router(); //라우터

const AuthMiddleWare = require("../middleware/auth"); //인증 미들웨어

router
.post("/", AuthMiddleWare, async(req, res) => { //프로필 생성
    const {name, sourceLanguage } = req.body;

    if(!name){ //프로필 이름 입력 안했을 때
        return res.status(400).json({
            message : "필수입력 사항을 모두 입력해주세요."
        });
    }

    if(2 < name.length || name.length > 25){
        res.status(400).json({
            message : "프로필 이름은 2자 이상 25자 이하까지만 입력가능합니다."
        });
    }
    
    try{
        //프로필 개수 체크 (4개까지만 생성 제한)
        const cntSql = `SELECT COUNT(*) as cnt 
                        FROM profiletbl 
                        WHERE uid = ?`

        const [count] = await pool.query(cntSql, [req.user.id]); //프로필 개수 확인 

        if(count[0].cnt >= 4){
            return res.status(400).json({
                message : "프로필은 4개까지만 생성가능합니다."
            });
        }

        //동일한 계정 내에서 프로필 이름 중복되는걸 방지(일단
        const checkSql = `SELECT name 
                            FROM profiletbl 
                            WHERE uid = ? AND name = ?`;

        const [exist] = await pool.query(checkSql, [req.user.id, name]);

        if(exist.length > 0){
            return res.status(400).json({
                message : "이미 동일한 이름의 프로필이 존재합니다."
            });
        }

        //프로필 생성 
        const createSql = `INSERT INTO profiletbl (uid, name, sourcelanguage)
                             VALUES (?, ?, ?)`;

        await pool.query(createSql, [req.user.id, name, sourceLanguage]);

        return res.status(201).json({
            message : "프로필 생성 성공"
        });

    }catch(err){
        console.error( `프로필 생성 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "프로필 생성을 실패했습니다."
        });
    }
})
.get("/", AuthMiddleWare, async(req, res) => { //프로필 조회
    try{
        const sql = `SELECT profile_id, name, sourcelanguage 
                     FROM profiletbl 
                     WHERE uid = ?`
        
        const [profiles] = await pool.query(sql, [req.user.id]);

        return res.status(200).json({
            message : "프로필 조회에 성공했습니다.",
            profiles
        });

    } catch (err){
        console.error(`프로필 조회 중 에러 발생 : ${err}`);

        return res.status(500).json({
            message : "프로필 조회를 실패했습니다."
        });
    }
})  
.patch("/:profileId", AuthMiddleWare, async(req, res) => { //프로필 정보 수정
    const {profileId} = req.params;
    const {name, sourceLanguage} = req.body;
    
    if(!name){
        return res.status(400).json({
            message : "필수사항을 모두 입력해주세요"
        });
    }

    if(2 < name.length || name.length > 25){
        return res.status(400).json({
            message : "프로필 이름은 2자 이상 25자 이하까지만 입력가능합니다."
        })
    }
    const allowedLanguage = ["ja", "zn", "en"];

    if(sourceLanguage && !allowedLanguage.includes(sourceLanguage)){
        return res.status(400).json({
            message : "지원하지 않는 언어입니다."
        });
    }

    try{
        //프로필 소유 검증
        const checkSql = `SELECT * 
                            FROM profiletbl 
                            WHERE uid = ? AND profile_id = ?`;

        const [profile] = await pool.query(checkSql, [req.user.id, profileId]);

        if(profile.length === 0){
            return res.status(403).json({
                message : "접근권한이 없는 사용자 입니다."
            });
        }   

        //정보 수정

        const sql = `UPDATE profiletbl
                     SET name = ?,
                        sourcelanguage = ?,
                        updated_at = NOW()
                    WHERE profile_id = ?`

        await pool.query(sql, [name, sourceLanguage, profileId]);

        return res.status(200).json({
            message : "프로필 수정에 성공했습니다."
        });

    }catch(err){
        console.error(`프로필 수정 중 에러 발생 : ${err}`);

        return res.status(500).json({
            message : "프로필 수정을 실패했습니다."
        });
    }
})
.delete("/:profileId", AuthMiddleWare,  async(req, res) => { //프로필 삭제
    const {profileId} = req.params;

    try{
        //프로필 소유 검증 
       const checkSql = `SELECT * FROM profiletbl WHERE uid = ? AND profile_id = ?`;
       const [profile] = await pool.query(checkSql, [req.user.id, profileId]);

       if(profile.length === 0){
        return res.status(403).json({
            message : "접근권한이 없는 사용자 입니다."
        });
       }

       //프로필 삭제
       const deleteSql = `DELETE FROM profiletbl WHERE profile_id = ?`;
       await pool.query(deleteSql, [profileId]);

       return res.status(200).json({
        message : "프로필 삭제에 성공했습니다."
       });

    } catch (err){
        console.error(`프로필 삭제 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "프로필 삭제를 실패했습니다."
        });
    }
});

module.exports = router;
