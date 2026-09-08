const pool = require("../database/maria"); //데이터 베이스 연결 풀
const express = require("express"); //express 모듈 
const bcrypt = require("bcrypt"); //bcrypt 모듈

const router = express.Router(); //라우터 객체

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/; 
//정규 표현식을 이용한 비밀번호 보안성 확인(8자 이상, 대문자, 소문자 1자 이상 포함, 특수문자 포함)

router
.post("/signUp", async(req, res) => {
    const {email, name, password} = req.body; //입력받은 인자들
    let conn; //db 연결 변수

    if(!email || !name || !password){ //하나라도 입력하지 않았을 경우
        return res.status(400).json({
            message : "필수 입력사항을 모두 입력해주세요."
        });
    }

    //이메일 중복 체크
    const emailCheck = `SELECT * FROM usertbl WHERE email = ?`;

    const [exist] = await pool.query(emailCheck, [email]);

    if(exist.length > 0){
        return res.status(400).json({
            message : "이미 존재하는 이메일 입니다."
        });
    }

    if(!PASSWORD_REGEX.test(password)){ //비밀번호 보안 준수 여부 확인
        return res.status(400).json({
            message : "비밀번호가 보안기준에 적합하지 않습니다.(8글자 이상, 대소문자 및 숫자, 특수기호 포함 필수)"
        });
    }

    const hashedPw = await bcrypt.hash(password, 10);

    const sql = `INSERT INTO usertbl (email, password, name)
                 VALUES (?, ?, ?)`;

    try{
        await pool.query(sql, [email, name, hashedPw]);

        return res.status(201).json({
            message : "회원가입을 성공하였습니다."
        });
    } catch(err) {
        console.error(`회원가입 중 오류 발생 : ${err}`);
        
        res.status(500).json({
            message : "죄송합니다. 회원가입에 실패했습니다."
        });
    }
    
})
.get("/EmailCheck", async(req, res) => {
    const email = req.query.email;

    if(!email){
        return res.status(400).json({
            message : "이메일은 필수 입력 입니다."
        });
    }

    const checkSql = `SELECT * FROM usertbl WHERE email = ?`;

    const check = await pool.query(checkSql, [email]);

    if(check.length > 0){
        res.status(400).json({
            message : "이미 가입이 된 이메일입니다."
        });
    };

    return res.status(200).json({
        message : "가입 가능한 이메일입니다."
    });
});
