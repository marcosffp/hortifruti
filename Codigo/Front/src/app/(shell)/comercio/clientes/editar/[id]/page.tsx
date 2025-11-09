import EditarClientePageContent from "./content";

interface PageProps {
  params: Promise<{ id: string; }>;
}

export default async function EditarClientePage({ params }: PageProps) {
  const { id } = await params;
  
  return <EditarClientePageContent id={id} />;
}
