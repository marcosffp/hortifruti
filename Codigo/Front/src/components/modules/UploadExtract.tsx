import Button from "../ui/Button";
import Card from "../ui/Card";

export default function UploadExtract() {
  return (
    <Card title="Carregar Arquivos PDF">
      <div className="border-2 border-dashed border-gray-300 rounded-lg p-10 text-center">
        <p className="text-gray-600 mb-4">
          Clique para selecionar arquivos ou arraste e solte PDFs aqui
        </p>
        <Button variant="primary">Selecionar Arquivos</Button>
      </div>
    </Card>
  );
}
