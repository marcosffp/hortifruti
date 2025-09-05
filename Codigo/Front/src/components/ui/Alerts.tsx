import { Alert, Stack } from "@mui/material";
import Card from "./Card";

export default function Alerts() {
  return (
    <Card title="Exemplos de Alertas">
      <Stack spacing={2}>
        <Alert severity="success">Este é um alerta de sucesso.</Alert>
        <Alert severity="info">Este é um alerta informativo.</Alert>
        <Alert severity="warning">Este é um alerta de aviso.</Alert>
        <Alert severity="error">Este é um alerta de erro.</Alert>
      </Stack>
    </Card>
  );
}
