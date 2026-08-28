//로그인 인증 
require("dotenv").config();

const jwt = require("jsonwebtoken");

const AuthMiddleWare = (req, res, next) => {

    //헤더에서 토큰 가져옴
    const authHeader = req.header[authHeader];
    const token = authHeader && authHeader.split(" ")[1];

    //토큰이 존재하지 않을 시 로그인 필요함 안내
    if(!token){
        res.status(400).json({
            message : "앱 이용을 위해서는 로그인이 필요합니다."
        });
    }

    try{
        const decode = jwt.verify(token, process.env.JWT_SECRET); //토큰 검증

        req.user =  decode;

        next();
    } catch(err){
        res.status(400).json({
            message : "유효하지 않은 토큰입니다. 다시 로그인해주세요."
        });
    }
}

module.exports = authMiddleware;