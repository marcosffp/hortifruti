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
