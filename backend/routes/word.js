const AuthMiddleWare = require("../middleware/auth"); //로그인 인증

const pool = require("../database/maria"); //db 연결

//router 관련 모듈
const express = require("express");
const router = express.Router();

//고유 명사 CURD
router
.get("/:profileId", AuthMiddleWare, async(req, res) => {

})
.post("/profileId", AuthMiddleWare, async(req, res) => {

})
.patch("/:profileId/:wordId", AuthMiddleWare, async(req, res) => {
    
})
.delete("/:profileId/:wordId", AuthMiddleWare, async(req, res) => {

});


module.exports = router;