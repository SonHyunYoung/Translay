const pool = require("../database/maria");

const profileOwner = async(req, res, next) => {
    try{
        //프로필 검증
        const checkSql = `SELECT profile_id 
                            FROM profiletbl 
                            WHERE profile_id = ? AND uid = ?`;

        const [rows] = await pool.query(checkSql, [req.params.profileId, req.user.id]);

        if(rows.length === 0){
            return res.status(400).json({
                message : "접근 권한이 없습니다."
            });
        }

        next();
    } catch(err){ //오류 발생
        console.error(`프로필 검증 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "프로필 검증에 실패했습니다."
        });
    }
}

module.exports = profileOwner;