import { clientService } from "@/services/clientService";
import { ClientSelectionInfo } from "@/types/clientType";
import { UserRoundSearch } from "lucide-react";
import { useEffect, useState } from "react";

type ClientSelectorProps = {
    onClientSelect?: (client: ClientSelectionInfo) => void;
};

export default function ClientSelector({ onClientSelect }: ClientSelectorProps) {
    const [clientes, setClientes] = useState<ClientSelectionInfo[]>([]);
    const [selectedId, setSelectedId] = useState<string>("none");

    useEffect(() => {
        const fetchClients = async () => {
            const data = await clientService.getAllClientsForSelection();
            setClientes(data);
        };
        fetchClients();
    }, []);

    const handleClientSelect = (event: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedId(event.target.value);
        const selectedClient = clientes.find(client => client.clientId === Number(event.target.value));
        if (selectedClient && onClientSelect) {
            onClientSelect(selectedClient);
        }
    };

    return (
        <div className="flex flex-col gap-4 p-6 bg-white">
            <h2 className="flex items-center gap-2 text-xl font-bold text-gray-800 mb-2">
                <UserRoundSearch className="w-6 h-6 text-green-600" />
                Selecionar Cliente
            </h2>
            <div className="flex flex-col gap-2">
                <label htmlFor="client" className="font-medium text-gray-700">
                    Cliente
                </label>
                <select
                    id="client"
                    className="border border-gray-300 rounded-md p-2 focus:outline-none focus:ring-2 focus:ring-green-500 transition w-full truncate"
                    value={selectedId}
                    onChange={handleClientSelect}
                >
                    <option value="none" disabled>Selecione um cliente</option>
                    {clientes.map(client => (
                        <option key={client.clientId} value={client.clientId}>
                            {client.clientName}
                        </option>
                    ))}
                </select>
            </div>
        </div>
    );
}