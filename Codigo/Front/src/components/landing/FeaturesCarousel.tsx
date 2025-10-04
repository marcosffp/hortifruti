import { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, CheckCircle, ArrowRight } from 'lucide-react';
import Link from 'next/link';

interface Feature {
  id: number;
  title: string;
  description: string;
  icon: React.ReactNode;
  image: string;
  benefits: string[];
}

interface FeaturesCarouselProps {
  features: Feature[];
}

export default function FeaturesCarousel({ features }: FeaturesCarouselProps) {
  const [currentFeature, setCurrentFeature] = useState(0);

  // Auto-play do carrossel
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentFeature((prev) => (prev + 1) % features.length);
    }, 5000);
    return () => clearInterval(interval);
  }, [features.length]);

  const nextFeature = () => {
    setCurrentFeature((prev) => (prev + 1) % features.length);
  };

  const prevFeature = () => {
    setCurrentFeature((prev) => (prev - 1 + features.length) % features.length);
  };

  return (
    <div className="relative">
      <div className="bg-white rounded-xl shadow-lg overflow-hidden max-w-5xl mx-auto">
        <div className="grid lg:grid-cols-2 gap-0">
          <div className="p-6 lg:p-8 flex flex-col justify-center">
            <div className="mb-4">
              <div className="inline-flex items-center justify-center w-12 h-12 bg-primary rounded-xl mb-4">
                {features[currentFeature].icon}
              </div>
              <h3 className="text-xl lg:text-2xl font-bold text-neutral-900 mb-3">
                {features[currentFeature].title}
              </h3>
              <p className="text-base text-neutral-600 mb-4">
                {features[currentFeature].description}
              </p>
            </div>

            <div className="space-y-2 mb-6">
              {features[currentFeature].benefits.map((benefit, index) => (
                <div key={index} className="flex items-center">
                  <CheckCircle className="w-4 h-4 text-primary mr-2 flex-shrink-0" />
                  <span className="text-sm text-neutral-700">{benefit}</span>
                </div>
              ))}
            </div>

          </div>

          <div className="relative h-48 lg:h-auto m-4 ml-0 rounded-lg overflow-hidden">
            <img 
              src={features[currentFeature].image} 
              alt={features[currentFeature].title}
              className="w-full h-full object-cover"
            />
          </div>
        </div>
      </div>

      {/* Navigation Controls - Arrows and Dots together */}
      <div className="flex justify-center items-center mt-6 space-x-4">
        <button
          onClick={prevFeature}
          className="bg-white hover:bg-neutral-50 p-2 rounded-full shadow-md transition-colors focus:outline-none focus:ring-2 focus:ring-primary cursor-pointer"
          aria-label="Funcionalidade anterior"
        >
          <ChevronLeft className="w-5 h-5 text-neutral-700" />
        </button>

        {/* Dots Indicator */}
        <div className="flex space-x-2">
          {features.map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentFeature(index)}
              className={`w-2 h-2 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary ${
                index === currentFeature ? 'bg-primary' : 'bg-neutral-300 hover:bg-neutral-400'
              }`}
              aria-label={`Ir para funcionalidade ${index + 1}`}
            />
          ))}
        </div>

        <button
          onClick={nextFeature}
          className="bg-white hover:bg-neutral-50 p-2 rounded-full shadow-md transition-colors focus:outline-none focus:ring-2 focus:ring-primary cursor-pointer"
          aria-label="Próxima funcionalidade"
        >
          <ChevronRight className="w-5 h-5 text-neutral-700" />
        </button>
      </div>
    </div>
  );
}