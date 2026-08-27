require("dotenv").config(); //env 파일 사용
const express = require("express"); // express 모듈 사용 선언

const port = process.env.PORT;
const app = express();

app.get("/", (req, res) => { //서버 작동확인
    return res.status(200).json({message : "서버 정상작동 확인"});
});

app.listen(port, () => {
    console.log(`${port}번 포트에서 정상작동 확인`);
});