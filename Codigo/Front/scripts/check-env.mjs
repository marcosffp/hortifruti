const apiUrl = process.env.NEXT_PUBLIC_API_URL;
const isProduction = process.env.NODE_ENV === "production";

if (!apiUrl) {
  console.error(
    "\nErro de build: a variável de ambiente NEXT_PUBLIC_API_URL não está definida.\n" +
      "Configure-a (veja .env.example) antes de rodar o build.\n",
  );
  process.exit(1);
}

if (isProduction && !apiUrl.startsWith("https://")) {
  console.error(
    `\nErro de build: NEXT_PUBLIC_API_URL="${apiUrl}" precisa começar com "https://" em produção.\n`,
  );
  process.exit(1);
}
