"use client";

import { RotateCcw, ZoomIn, ZoomOut } from "lucide-react";
import { useState } from "react";
import { round } from "@/utils/numericRow";

interface NotaImagePanelProps {
  imageUrl: string;
}

export default function NotaImagePanel({ imageUrl }: NotaImagePanelProps) {
  const [zoom, setZoom] = useState(1);

  return (
    <div className="md:w-1/2 flex flex-col border-b md:border-b-0 md:border-r border-gray-200 min-h-0">
      <div className="flex items-center gap-2 p-2 border-b border-gray-200 shrink-0">
        <button
          type="button"
          onClick={() => setZoom((z) => Math.max(1, round(z - 0.25, 2)))}
          className="p-2 hover:bg-gray-100 rounded-lg"
          title="Diminuir zoom"
        >
          <ZoomOut className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => setZoom((z) => Math.min(4, round(z + 0.25, 2)))}
          className="p-2 hover:bg-gray-100 rounded-lg"
          title="Aumentar zoom"
        >
          <ZoomIn className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => setZoom(1)}
          className="p-2 hover:bg-gray-100 rounded-lg"
          title="Resetar zoom"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
        <span className="text-xs text-gray-500">
          {Math.round(zoom * 100)}% — role pra navegar quando ampliado
        </span>
      </div>
      <div className="flex-1 overflow-auto bg-gray-100 p-2">
        {/* biome-ignore lint: preview de dev, não precisa de otimização do next/image */}
        <img
          src={imageUrl}
          alt="Nota original"
          style={{ width: `${zoom * 100}%`, maxWidth: "none" }}
          className="select-none"
          draggable={false}
        />
      </div>
    </div>
  );
}
