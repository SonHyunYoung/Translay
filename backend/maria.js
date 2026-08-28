//db 연결 
require("dotenv").config(); //env 파일 연결
const db = require("mysql"); //db연결 모듈

const conn = db.createConnection({
    host : process.env.DB_HOST,
    port : process.env.DB_PORT,
    user : process.env.DB_USER,
    password : process.env.DB_PW,
    database : process.env.DB_NAME
})

module.export = conn;