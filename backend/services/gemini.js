const LANGUAGE_NAMES = { ja: '일본어', zh: '중국어', en: '영어' };

async function translate({ text, sourceLanguage, glossary, speaker }) {
  const prompt = buildPrompt({ text, sourceLanguage, glossary, speaker });

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 30000);
  const t0 = Date.now();

  try {
    const res = await fetch(`${process.env.LLM_BASE_URL}/v1/gateway/chat/completions/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${process.env.LLM_API_KEY}`
      },
      body: JSON.stringify({
        model: process.env.LLM_MODEL || 'gemini-3.5-flash-lite',
        messages: [{ role: 'user', content: prompt }],
        temperature: 0.3
      }),
      signal: controller.signal
    });

    if (!res.ok) {
      throw new Error(`LLM ${res.status}: ${await res.text()}`);
    }
    const data = await res.json();
    console.log(`[LLM] ${Date.now() - t0}ms`);

    return (data.choices?.[0]?.message?.content || '').trim();
  } finally {
    clearTimeout(timer);
  }
}

/**
 * OCR로 읽은 이름표가 한자를 잘못 인식했을 때,
 * 등록된 캐릭터 목록 중 같은 인물을 LLM이 고르게 한다.
 * - matchSpeaker(로컬)가 실패한 경우에만 호출된다.
 * - 번역용과 다른 가벼운 모델(LLM_NAME_MODEL)을 쓴다.
 * - candidates: [{ source: 원문 이름, target: 한국어 번역명 }, ...]
 * - 반환: 선택된 candidate 객체 또는 null(없음/실패/타임아웃)
 */
async function resolveSpeaker({ ocrName, candidates }) {
  if (!ocrName || !candidates || candidates.length === 0) return null;

  const list = candidates.map((c, i) => `${i + 1}. ${c.source} (${c.target})`).join('\n');
  const prompt = `게임 화면의 캐릭터 이름표를 OCR로 읽었는데, 글자가 한두 개 잘못 인식되었을 수 있습니다.
(예: 획이 많은 한자가 모양이 비슷한 다른 한자로 읽히거나, 일부 글자가 빠짐)
아래 등록된 캐릭터 중에서 OCR 결과가 가리키는 인물을 하나 고르세요.

[OCR 결과]
${ocrName}

[등록된 캐릭터]  (번호. 원문 이름 (한국어 이름))
${list}

규칙:
- OCR 결과와 글자 모양이 닮았거나 글자 수·구성이 비슷한 후보를 가장 가까운 순으로 판단해 하나를 고르세요.
- 닮은 후보가 하나도 없을 때만 NONE 이라고 출력하세요.
- 출력은 후보의 번호 숫자 하나, 또는 NONE 만 쓰세요. 설명이나 다른 글자는 절대 쓰지 마세요.`;

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  const t0 = Date.now();

  try {
    const res = await fetch(`${process.env.LLM_BASE_URL}/v1/gateway/chat/completions/`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${process.env.LLM_API_KEY}`
      },
      body: JSON.stringify({
        model: process.env.LLM_NAME_MODEL || 'gemini-3.5-flash-lite',
        messages: [{ role: 'user', content: prompt }],
        temperature: 0
      }),
      signal: controller.signal
    });

    if (!res.ok) {
      throw new Error(`LLM ${res.status}: ${await res.text()}`);
    }
    const data = await res.json();
    const out = (data.choices?.[0]?.message?.content || '').trim();
    console.log(`[이름추론] ${Date.now() - t0}ms, 후보 ${candidates.length}명, '${ocrName}' → 응답='${out}'`);

    if (!out || /^none/i.test(out)) return null;

    const m = out.match(/\d+/);
    if (!m) return null;
    const idx = parseInt(m[0], 10) - 1;
    return candidates[idx] || null;

  } catch (e) {
    console.error(`[이름추론] 실패: ${e.message || e}`);
    return null;
  } finally {
    clearTimeout(timer);
  }
}

function buildPrompt({ text, sourceLanguage, glossary, speaker }) {
  const language = LANGUAGE_NAMES[sourceLanguage] || sourceLanguage || '일본어';

  const speakerBlock = speaker ? `【현재 화자】\n${speaker}\n` : '';

  let wordBlock = '';
  if (glossary && glossary.length > 0) {
    wordBlock = '【고유명사】\n';
    wordBlock += glossary.map(w => `- ${w.source} → ${w.target}`).join('\n');
  }

  const speakerRule = speaker
    ? '\n6. 이 대사는 위 【현재 화자】가 말하는 것입니다. 화자의 입장에 맞는 어투로 번역하세요.'
    : '';

  return `당신은 ${language}를 한국어로 번역하는 전문가입니다.
아래 규칙을 반드시 따르세요:
1. 원문 안에 이미 한국어로 바뀐 호칭이 섞여 있을 수 있습니다. 이 단어들은 절대 수정하거나 다른 표기로 바꾸지 말고 그대로 유지하세요.
2. 그 단어에 붙는 조사(은/는/이/가 등)는 받침에 맞게 자연스럽게 쓰세요.
3. 목록에 없는 인물 이름을 임의로 만들어내지 마세요.
4. 이 텍스트는 OCR로 추출되어 한자가 일부 잘못 인식되었을 수 있습니다. 문맥상 어색한 글자가 있으면 자연스럽게 추정해서 번역하세요.
5. 번역 결과만 출력하고, 설명이나 부연은 절대 추가하지 마세요.${speakerRule}

${speakerBlock}
${wordBlock}

【번역할 텍스트】
${text}`;
}

module.exports = { translate, resolveSpeaker };