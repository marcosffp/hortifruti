import EditarClientePageContent from "./content";

interface PageProps {
  readonly params: { id: string; } | Promise<{ id: string; }>;
}

export default async function EditarClientePage({ params }: PageProps) {
  const { id } = await Promise.resolve(params);
  
  return <EditarClientePageContent id={id} />;
}
