type Props = {
    title: string;
    cols: string[];
};

export default function SkeletonTableLoading({ title, cols }: Props) {
    return (
        <div>
            <h2 className="text-lg font-semibold text-gray-800 mb-4">{title}</h2>
            <table className="min-w-full border border-gray-200">
                <thead>
                    <tr>
                        {cols.map((col) => (
                            <th
                                key={col}
                                className="px-4 py-2 border-b border-gray-200 bg-gray-50 text-left text-xs font-semibold text-gray-700"
                            >
                                {col}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {[...Array(5)].map((_, idx) => (
                        <tr key={idx} className="animate-pulse">
                            {cols.map((_, colIdx) => (
                                <td className="px-4 py-2 border-b border-gray-100" key={colIdx}>
                                    <div className="h-4 w-20 bg-gray-200 rounded opacity-60" />
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}