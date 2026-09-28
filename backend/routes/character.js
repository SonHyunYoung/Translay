const AuthMiddleWare = require("../middleware/auth"); //로그인 검증
 
const pool = require("../database/maria"); //db연결

const express = require("express"); //라우터 모듈
const router = express.Router(); //라우터

//캐릭터 이름 등록
router
.get()
.post()
.patch()
.delete();

//캐릭터 호칭 등록
router
.get()
.post()
.patch()
.delete();

module.exports = router;