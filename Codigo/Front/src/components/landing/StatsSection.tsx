import { useState, useEffect } from 'react';

interface StatItem {
  number: string;
  label: string;
  description: string;
}

const stats: StatItem[] = [
  {
    number: "50+",
    label: "Clientes Ativos",
    description: "Estabelecimentos que confiam na nossa solução"
  },
  {
    number: "40%",
    label: "Redução de Perdas",
    description: "Média de redução em desperdícios"
  },
  {
    number: "60%",
    label: "Economia de Tempo",
    description: "Menos tempo gasto em tarefas administrativas"
  },
  {
    number: "24/7",
    label: "Suporte Disponível",
    description: "Atendimento sempre que você precisar"
  }
];

export default function StatsSection() {
  const [counters, setCounters] = useState<Record<string, number>>({});
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !isVisible) {
          setIsVisible(true);
          // Animate counters
          stats.forEach((stat, index) => {
            if (stat.number.includes('%')) {
              const targetNumber = parseInt(stat.number.replace('%', ''));
              animateCounter(index, targetNumber, '%');
            } else if (stat.number.includes('+')) {
              const targetNumber = parseInt(stat.number.replace('+', ''));
              animateCounter(index, targetNumber, '+');
            }
          });
        }
      },
      { threshold: 0.5 }
    );

    const section = document.getElementById('stats-section');
    if (section) observer.observe(section);

    return () => observer.disconnect();
  }, [isVisible]);

  const animateCounter = (index: number, target: number, suffix: string) => {
    let current = 0;
    const increment = target / 50;
    const timer = setInterval(() => {
      current += increment;
      if (current >= target) {
        current = target;
        clearInterval(timer);
      }
      setCounters(prev => ({
        ...prev,
        [index]: Math.floor(current)
      }));
    }, 40);
  };

  return (
    <section id="stats-section" className="py-20 bg-primary">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
          {stats.map((stat, index) => (
            <div key={index} className="text-center text-white">
              <div className="text-4xl lg:text-5xl font-bold mb-2">
                {stat.number.includes('%') 
                  ? `${counters[index] || 0}%`
                  : stat.number.includes('+')
                    ? `${counters[index] || 0}+`
                    : stat.number
                }
              </div>
              <div className="text-xl font-semibold mb-1 text-white/90">
                {stat.label}
              </div>
              <div className="text-sm text-white/70">
                {stat.description}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}