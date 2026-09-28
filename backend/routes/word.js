const AuthMiddleWare = require("../middleware/auth"); //로그인 인증

const pool = require("../database/maria"); //db 연결

//router 관련 모듈
const express = require("express");
const router = express.Router();

//고유 명사 CURD
router
.get("/:profileId", AuthMiddleWare, async(req, res) => {
    const {profileId} = req.params;
    
    try{
        const [sql] = `SELECT * FROM charactertbl WHERE profild_id = ?`;

        await pool.query(sql, [profileId]);

        if(sql.length === 0){
            return res.status(400).json({
                message : "불러올 데이터가 없습니다."
            });
        }
    } catch(err){

    }
})
.post("/profileId", AuthMiddleWare, async(req, res) => {

})
.patch("/:profileId/:wordId", AuthMiddleWare, async(req, res) => {
    
})
.delete("/:profileId/:wordId", AuthMiddleWare, async(req, res) => {

});


module.exports = router;