const pool = require("../database/maria");

const CharacterOwner = async(req, res, next) => { //해당 호칭이 캐릭터에 속한 호칭인지 확인
    try{
        const checkSql = `SELECT character_id 
                            FROM charactertbl
                            WHERE character_id = ? AND profile_id = ?`;

        const [rows] = await pool.query(checkSql, [req.params.characterID, req.params.profileId]);

        if(rows.length === 0){
            return res.status(404).json({
                message : "캐릭터를 찾을 수 없습니다."
            });
        }

        next();

    } catch(err) {
        console.error(`캐릭터 검증 중 오류 발생 : ${err}`);

        return res.status(500).json({
            message : "호칭 조회 중 오류가 발생했습니다."
        });
    }
}

module.exports = CharacterOwner;