const pool = require("../database/maria"); //데이터 베이스 연결 풀
const express = require("express"); //express 모듈 
const bcrypt = require("bcrypt"); //bcrypt 모듈
const jwt = require("jsonwebtoken"); //jwt 모듈

const router = express.Router(); //라우터 객체

//회원가입 API
const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/; 
//정규 표현식을 이용한 비밀번호 보안성 확인(8자 이상, 대문자, 소문자 1자 이상 포함, 특수문자 포함)

router
.post("/signUp", async(req, res) => {
    const {email, name, password} = req.body; //입력받은 인자들

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

    //비밀번호 보안 준수 여부 확인
    if(!PASSWORD_REGEX.test(password)){ 
        return res.status(400).json({
            message : "비밀번호가 보안기준에 적합하지 않습니다.(8글자 이상, 대소문자 및 숫자, 특수기호 포함 필수)"
        });
    }

    const hashedPw = await bcrypt.hash(password, 10); //비밀번호 해쉬

    const sql = `INSERT INTO usertbl (email, password, name)
                 VALUES (?, ?, ?)`; //가입자 추가 sql문

    //회원가입 쿼리 
    try{ 
        await pool.query(sql, [email, name, hashedPw]);

        return res.status(201).json({
            message : "회원가입을 성공하였습니다."
        });
    } catch(err) { //회원가입 실패
        console.error(`회원가입 중 오류 발생 : ${err}`);
        
        res.status(500).json({
            message : "죄송합니다. 회원가입에 실패했습니다."
        });
    }
    
})
.get("/EmailCheck", async(req, res) => { //이메일 중복 체크 api
    const email = req.query.email;

    if(!email){ //이메일 존재하지 않을 때
        return res.status(400).json({
            message : "이메일은 필수 입력 입니다."
        });
    }

    //이메일 존재여부 검색
    const checkSql = `SELECT * FROM usertbl WHERE email = ?`; 

    const check = await pool.query(checkSql, [email]); 

    //주석 확인
    if(check.length > 0){ 
        res.status(409).json({
            message : "이미 가입이 된 이메일입니다."
        });
    } 

    //가입 성공
    return res.status(200).json({
        message : "가입이 가능한 이메일입니다."
    });

});


//로그인 API (로그아웃은 프론트에서 토큰을 삭제하는 방식으로 구현 가능하기 때문에 api 구현 x)
router
.post("/login", async(req, res) => {
    const {email, password} = req.body;

    if(!email || !password) { //이메일 혹은 비밀번호를 입력하지 않았을 경우
        return res.status(400).json({
            message : "이메일 혹은 비밀번호를 입력해주세요."
        });
    }

    try{ //유저 확인용 쿼리

        const userInfo = pool.query("SELECT * FROM usertbl WHERE email = ?", [email]);

        if(userInfo.length === 0){ //유저 정보가 없을 경우
            return res.status(400).json({
                message : "이메일 혹은 비밀번호가 틀렸습니다."
            });
        }

        const isMatch = await bcrypt.compare(password, userInfo.password);
        
        if(!isMatch){ //비밀번호 틀린 경우
            return res.status(400).json({
                message : "이메일 혹은 비밀번호가 틀렸습니다."
            });
        }

        //로그인 성공
        const token = jwt.sign(
            {id : userInfo.id, 
            email : userInfo.email,
            },
            process.env.JWT_SECRET,
            {expiresIn : "30d"}
        );

        return res.status(200).json({
            user_id : userInfo.email,
            message : "로그인 성공",
            token : token
        });
    } catch(err){
        console.log(`로그인 중 오류가 발생 : ${err}`);

        return res.status(500).json({
            message : "로그인 중 서버 오류 발생"
        });
    }

});