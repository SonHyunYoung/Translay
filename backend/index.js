require("dotenv").config(); //env 파일 사용
const express = require("express"); // express 모듈 사용 선언

const port = process.env.PORT;
const app = express();

app.use(express.json()); //json 읽어올 수 있게 선언

app.use("/api/auth", require("./routes/auth")); //유저 관련 API 라우터
app.use("/api/translate", require("./routes/translate")); //번역 관련 API 라우터

app.get("/", (req, res) => { //서버 작동확인
    return res.status(200).json({message : "서버 정상작동 확인"});
});

app.listen(port, () => {
    console.log(`${port}번 포트에서 정상작동 확인`);
});