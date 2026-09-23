//프로필 관련 CRUD

const pool = require("../database/maria"); //db 연결 

const express = require("express"); //express 모듈
const router = express.Router(); //라우터

const AuthMiddleWare = require("../middleware/auth"); //인증 미들웨어

router
.post("/", AuthMiddleWare, async(req, res) => { //프로필 생성
    
})
.get("/", AuthMiddleWare, async(req, res) => { //프로필 조회
    
})
.patch("/:profileId", AuthMiddleWare, async(req, res) => { //프로필 정보 수정

})
.delete("/:profileId", AuthMiddleWare,  async(req, res) => { //프로필 삭제

});

module.exports = router;
