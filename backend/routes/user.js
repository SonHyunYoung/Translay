const pool = require("../database/maria"); //db 연결

const AuthMiddleWare = require("../middleware/auth"); //인증 미들웨어

//express 모듈
const express = require("express");
const router = express.Router();

const bcrypt = require("bcrypt"); //암호화 모듈

//비밀번호 보안검증
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/; 

router
.get("/mypage", AuthMiddleWare, async(req, res) => {
    const uid = req.user.id;

    try{
        //불러오기
        const getSql = `SELECT email, name
                        FROM usertbl
                        WHERE id = ?`;
        
        const [user] = await pool.query(getSql, [uid]);

        return res.status(200).json({
            message : "유저 정보를 성공적으로 조회했습니다.",
            user : user[0]
        });

    } catch(err) {
        console.error(`유저 정보 불러오기 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "유저 정보 불러오기에 실패했습니다.",
        });
    }
})
.patch("/mypage/name", AuthMiddleWare, async(req, res) => { //닉네임 변경
    const uid = req.user.id;
    const { name } = req.body;

    if(!name){
        return res.status(400).json({
            message : "닉네임을 입력해주세요."
        });
    }

    if(name.length <2 || name.length > 15){
        return res.status(400).json({
            message : "닉네임은 2자 이상 15자 이하로 입력가능합니다."
        });
    }

    try{
        const updateSql = `UPDATE usertbl
                            SET name = ?, updated_at = NOW()
                            WHERE id = ?`;
        
        await pool.query(updateSql, [name, uid]);

        return res.status(200).json({
            message : "수정을 성공했습니다."
        });

    } catch(err) {
        console.error(`유저 닉네임 수정 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "수정에 실패했습니다."
        });
    }
})
.patch("/mypage/password", AuthMiddleWare, async(req, res) => { //비밀번호 변경
    const {currentPassword, newPassword} = req.body;
    const uid = req.user.id;

    if(!currentPassword || !newPassword){
        return res.status(400).json({
            message : "필수사항을 모두 입력해주세요."
        });
    }

    try{
        //비밀번호  체크
        const getSql = `SELECT password 
                        FROM usertbl
                        WHERE id = ?`;
        
        const [password] = await pool.query(getSql, [uid]);

        const isMatch = await bcrypt.compare(currentPassword, password[0].password);

        if(!isMatch){
            return res.status(400).json({
                message : "비밀번호가 일치하지 않습니다."
            });
        }

        //새 비밀번호가 현재 비밀번호와 동일한지 체크
        if(newPassword === currentPassword){
            return res.status(400).json({
                message : "현재 비밀번호와 동일한 비밀번호로 변경은 불가합니다."
            });
        }

        //새 비밀번호 보안기준 체크
        if(!PASSWORD_REGEX.test(newPassword)){
            return res.status(400).json({
                message : "비밀번호가 보안기준에 맞지 않습니다."
            });
        }

        //비밀번호 해쉬
        const hashedPw = await bcrypt.hash(newPassword, 10);

        //비밀번호 업데이트
        const updateSql = `UPDATE usertbl
                            SET password = ?, updated_at = NOW()
                            WHERE id = ?`;
        
        await pool.query(updateSql, [hashedPw, uid]);

        return res.status(200).json({
            message : "비밀번호 변경에 성공했습니다."
        });


    } catch(err) {
        console.error(`비밀번호 수정 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "비밀번호 변경을 실패했습니다."
        });
    }
})
.delete("/mypage", AuthMiddleWare, async(req, res) => {
    const uid = req.user.id;
    const {password} = req.body;

    if(!password){
        return res.status(400).json({
            message : "비밀번호를 입력해주세요."
        });
    }

    try{
        //비밀번호 체크
        const getSql = `SELECT password
                        FROM usertbl
                        WHERE id = ?`;

        const [userPw] = await pool.query(getSql, [uid]);

        const isMatch = await bcrypt.compare(password, userPw[0].password);

        if(!isMatch){
            return res.status(400).json({
                message : "잘못된 비밀번호 입니다."
            });
        }

        //삭제
        const deleteSql = `DELETE FROM usertbl
                            WHERE id = ?`;

        await pool.query(deleteSql, [uid]);

        return res.status(200).json({
            message : "회원탈퇴에 성공했습니다."
        });
        
    } catch(err){
        console.error(`유저 삭제 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "회원탈퇴에 실패했습니다."
        });
    }
});

module.exports = router;