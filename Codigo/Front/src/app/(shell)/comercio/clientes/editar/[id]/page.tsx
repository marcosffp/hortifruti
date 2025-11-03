import EditarClientePageContent from "./content";

interface PageProps {
  readonly params: { id: string; };
}

export default async function EditarClientePage({ params }: PageProps) {
  const { id } = params;
  
  return <EditarClientePageContent id={id} />;
}
