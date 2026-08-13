import RoleGuard from "@/components/auth/RoleGuard";
import EditarClientePageContent from "./content";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function EditarClientePage({ params }: PageProps) {
  const { id } = await params;

  return (
    <RoleGuard roles={["MANAGER", "EMPLOYEE"]}>
      <EditarClientePageContent id={id} />
    </RoleGuard>
  );
}
