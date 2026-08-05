const apiUrl = process.env.NEXT_PUBLIC_API_URL;
const backendUrl = process.env.BACKEND_URL;
const isProduction = process.env.NODE_ENV === "production";

if (!apiUrl) {
  console.error(
    "\nErro de build: a variável de ambiente NEXT_PUBLIC_API_URL não está definida.\n" +
      "Configure-a (veja .env.example) antes de rodar o build.\n",
  );
  process.exit(1);
}

if (!backendUrl) {
  console.error(
    "\nErro de build: a variável de ambiente BACKEND_URL não está definida.\n" +
      "É usada pelo rewrite em next.config.ts para proxear /api/* até o backend.\n" +
      "Configure-a (veja .env.example) antes de rodar o build.\n",
  );
  process.exit(1);
}

if (isProduction && !apiUrl.startsWith("/")) {
  console.error(
    `\nErro de build: NEXT_PUBLIC_API_URL="${apiUrl}" precisa ser um caminho relativo (ex.: "/api") em produção.\n` +
      "As chamadas devem passar pelo rewrite same-origin do Next.js — veja next.config.ts.\n",
  );
  process.exit(1);
}

if (isProduction && !backendUrl.startsWith("https://")) {
  console.error(
    `\nErro de build: BACKEND_URL="${backendUrl}" precisa começar com "https://" em produção.\n`,
  );
  process.exit(1);
}

// NEXT_PUBLIC_WS_URL (pareamento de dispositivo + fila de notas em tempo real) é opcional — sem
// ela, a página só perde a atualização automática e continua funcionando via refresh manual. Mas
// se alguém a definir com "ws://" numa build de produção (servida em https://), o navegador
// bloqueia a conexão por mixed content sem erro nenhum visível — silenciosamente quebrado. Falhar
// o build aqui é mais barato que descobrir isso em produção.
const wsUrl = process.env.NEXT_PUBLIC_WS_URL;
if (isProduction && wsUrl && !wsUrl.startsWith("wss://")) {
  console.error(
    `\nErro de build: NEXT_PUBLIC_WS_URL="${wsUrl}" precisa começar com "wss://" em produção ` +
      '(uma página servida em https:// não pode abrir WebSocket "ws://" — mixed content).\n',
  );
  process.exit(1);
}
