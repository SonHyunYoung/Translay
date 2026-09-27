const AuthMiddleWare = require("../middleware/auth"); //로그인 인증

const pool = require("../database/maria"); //db 연결

//router 관련 모듈
const express = require("express");
const router = express.Router();

module.exports = router;