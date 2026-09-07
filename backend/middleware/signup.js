const pool = require("../database/maria"); //데이터 베이스 연결 풀
const express = require("express"); //express 모듈 
const bcrypt = require("bcrypt"); //bcrypt 모듈

const router = express.Router(); //라우터 객체

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/; 
//정규 표현식을 이용한 비밀번호 보안성 확인(8자 이상, 대문자, 소문자 1자 이상 포함, 특수문자 포함)

router
.post("/signUp", async(req, res) => {
    const {email, name, password, confirmPw} = req.body; //입력받은 인자들
    let conn; //db 연결 변수

    if(!email || !name || !password || confirmPw){ //하나라도 입력하지 않았을 경우
        res.status(400).json({
            message : "필수 입력사항을 모두 입력해주세요."
        });
    }

    if(!PASSWORD_REGEX.test(password)){ //비밀번호 보안 준수 여부 확인
        res.status(400).json({
            message : "비밀번호는 대소문자, 숫자, 특수기호 포함한 8글자 이상이어야합니다."
        });
    }

    if(password != confirmPw){ //확인용 비밀번호가 일치하지 않는 경우
        res.status(400).json({ 
            error : "비밀번호가 일치하지 않습니다."
        });
    }

    const hashedPw = await bcrypt.hash(password, 10);

    let emailCheck = `SELECT * FROM usertbl WHERE email = ?`;

    const exist = await pool.query(emailCheck, () => {

    })
    
})
.get("/EmailCheck", async(req, res) => {
    const email = req.body;

    if(!email){
        res.status(400).json({
            message : "이메일은 필수 입력 입니다."
        });
    }

    let checkSql = `SELECT * FROM usertbl WHERE email = ?`;

    const check = pool.query(checkSql, [email], (err) => {

    });

    if(check.length > 0){
        res.status(400).json({
            message : "이미 가입이 된 이메일입니다."
        });
    };
});
