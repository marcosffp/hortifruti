type LoadingProps = {
  // Cobre o container mais próximo com position: relative (ex.: um card com
  // upload em andamento). Sem isso, position: absolute sobe até o ancestral
  // posicionado mais próximo — na ausência de um, cobre a viewport inteira e
  // bloqueia cliques em qualquer coisa embaixo (inclusive a navegação lateral).
  overlay?: boolean;
};

export default function Loading({ overlay = false }: LoadingProps) {
  const spinner = (
    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-green-500"></div>
  );

  if (overlay) {
    return (
      <div className="absolute inset-0 flex items-center justify-center z-50 rounded-lg">
        {spinner}
      </div>
    );
  }

  return <div className="flex items-center justify-center">{spinner}</div>;
}
