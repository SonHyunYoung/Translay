//gemini API 사용 선언
const { GoogleGenAI } = require('@google/genai');

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

async function translate({ text, sourceLanguage, glossary, characters, context }) {
  const prompt = buildPrompt({ text, sourceLanguage, glossary, characters, context });

  const interaction = await ai.interactions.create({
    model: 'gemini-3.5-flash-lite', 
    input: prompt,
  });

  return interaction.output_text;
}

//프롬프트 조립
function buildPrompt({ text, sourceLanguage, glossary, characters, context }) {
  // 1. 캐릭터 용어 블록 조립
  let characterBlock = '';
  if (characters && characters.length > 0) {
    characterBlock = '【등장인물】\n';

    characters.forEach(c => {
      characterBlock += `- ${c.source_name} → ${c.target_name}\n`;

      if (c.terms && c.terms.length > 0) {
        c.terms.forEach(t => {
          const desc = t.description ? ` (${t.description})` : '';
          characterBlock += `  · ${t.source} → ${t.target}${desc}\n`;
        });
      }

    });
  }

  // 2. 단어 용어 블록 조립 (지명, 아이템 등)
  let wordBlock = '';

  if (glossary && glossary.length > 0) {
    wordBlock = '【고유명사】\n';
    wordBlock += glossary.map(w => `- ${w.source} → ${w.target}`).join('\n');
  }

  // 3. 문맥 블록 조립 (Room DB에서 앱이 보내준 최근 3~5개)
  let contextBlock = '';

  if (context && context.length > 0) {

    contextBlock = '【이전 대사 (문맥 참고용)】\n';
    contextBlock += context.map(c => `- ${c.source_text} → ${c.translated_text}`).join('\n');

  }

  return `당신은 ${sourceLanguage}를 한국어로 번역하는 전문가입니다.
아래 규칙을 반드시 따르세요:
1. 등장인물 이름과 고유명사는 반드시 지정된 번역어를 사용하세요.
2. 이전 대사의 흐름을 자연스럽게 이어받아 번역하세요.
3. 번역 결과만 출력하고, 설명이나 부연은 절대 추가하지 마세요.

${characterBlock}
${wordBlock}
${contextBlock}

【번역할 텍스트】
${text}`;
}

module.exports = { translate };