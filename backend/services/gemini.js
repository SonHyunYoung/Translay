const { GoogleGenAI } = require('@google/genai');

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

const LANGUAGE_NAMES = { ja: '일본어', zh: '중국어', en: '영어' };

async function translate({ text, sourceLanguage, glossary, context, speaker }) {
  const prompt = buildPrompt({ text, sourceLanguage, glossary, context, speaker });

  const interaction = await ai.interactions.create({
    model: 'gemini-3.5-flash-lite',
    input: prompt,
  });

  return interaction.output_text;
}

function buildPrompt({ text, sourceLanguage, glossary, context, speaker }) {
  const language = LANGUAGE_NAMES[sourceLanguage] || sourceLanguage || '일본어';

  // 현재 화자 (있을 때만)
  const speakerBlock = speaker ? `【현재 화자】\n${speaker}\n` : '';

  // 단어 용어 블록 (지명, 아이템 등)
  let wordBlock = '';
  if (glossary && glossary.length > 0) {
    wordBlock = '【고유명사】\n';
    wordBlock += glossary.map(w => `- ${w.source} → ${w.target}`).join('\n');
  }

  const speakerRule = speaker
    ? '\n7. 이 대사는 위 【현재 화자】가 말하는 것입니다. 화자의 입장에 맞는 어투로 번역하세요.'
    : '';

  return `당신은 ${language}를 한국어로 번역하는 전문가입니다.
아래 규칙을 반드시 따르세요:
1. 원문 안에 이미 한국어로 바뀐 인물 이름과 호칭이 섞여 있습니다. 이 단어들은 절대 수정하거나 다른 표기로 바꾸지 말고 그대로 유지하세요.
2. 그 단어에 붙는 조사(은/는/이/가 등)는 받침에 맞게 자연스럽게 쓰세요.
3. 목록에 없는 인물 이름을 임의로 만들어내지 마세요.
4. 이 텍스트는 OCR로 추출되어 한자가 일부 잘못 인식되었을 수 있습니다. 문맥상 어색한 글자가 있으면 자연스럽게 추정해서 번역하세요.
5. 번역 결과만 출력하고, 설명이나 부연은 절대 추가하지 마세요.${speakerRule}

${speakerBlock}
${wordBlock}

【번역할 텍스트】
${text}`;
}

module.exports = { translate };