"use client";

import { useState } from 'react';
import { 
  ShoppingCart, 
  TruckIcon, 
  DollarSign, 
  Users, 
  BarChart3, 
  Shield,
  CheckCircle,
  ArrowRight,
  Play,
  Menu,
  X,
  LogIn,
  Star,
  Zap,
  Award,
  Phone
} from 'lucide-react';
import Link from 'next/link';
import FeaturesCarousel from '@/components/landing/FeaturesCarousel';
import StatsSection from '@/components/landing/StatsSection';
import SmoothScroll from '@/components/landing/SmoothScroll';

// Dados do carrossel de funcionalidades
const features = [
  {
    id: 1,
    title: "Gestão de Estoque Inteligente",
    description: "Controle completo do seu estoque com alertas de produtos em falta e relatórios detalhados de movimentação.",
    icon: <ShoppingCart className="w-12 h-8 text-white" />,
    image: "/tela-upload-extrato.jpg",
    benefits: ["Alertas automáticos", "Controle de validade", "Relatórios detalhados"]
  },
  {
    id: 2,
    title: "Cálculo de Frete Automático",
    description: "Sistema integrado para cálculo de frete baseado na localização do cliente, otimizando entregas.",
    icon: <TruckIcon className="w-12 h-8 text-white" />,
    image: "/tela-cálculo-frete.jpg",
    benefits: ["Cálculo automático", "Múltiplas transportadoras", "Rastreamento em tempo real"]
  },
  {
    id: 3,
    title: "Gestão Financeira Completa",
    description: "Controle total das finanças com relatórios de vendas, despesas e análise de lucratividade.",
    icon: <DollarSign className="w-12 h-8 text-white" />,
    image: "/tela-lançamento.jpg",
    benefits: ["Relatórios financeiros", "Análise de lucratividade", "Controle de fluxo de caixa"]
  },
  {
    id: 4,
    title: "Gestão de Clientes",
    description: "Cadastro completo de clientes com histórico de compras e programa de fidelidade.",
    icon: <Users className="w-12 h-8 text-white" />,
    image: "/tela-gestão-cliente.jpg",
    benefits: ["Cadastro completo", "Histórico de compras", "Programa de fidelidade"]
  },
  {
    id: 5,
    title: "Criação de Usuários",
    description: "Sistema completo de gerenciamento de usuários com diferentes níveis de acesso e permissões.",
    icon: <BarChart3 className="w-12 h-8 text-white" />,
    image: "/tela-criar-user.jpg",
    benefits: ["Controle de acesso", "Níveis de permissão", "Auditoria de ações"]
  }
];

export default function LandingPage() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <div className="min-h-screen bg-white">
      <SmoothScroll />
      {/* Header/Navigation */}
      <header className="bg-primary shadow-sm border-b border-neutral-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-4">
              <img src="/icon.png" alt="Hortifruti SL" className="h-9 w-auto" />
              <span className="text-2xl font-bold text-white">Hortifruti</span>
            </div>

            {/* Phone number - visible on desktop */}
            <div className="hidden md:flex items-center space-x-6">
              <a href="tel:+5531364970064" className="flex items-center space-x-2 text-white hover:text-white/80 transition-colors">
                <Phone className="w-4 h-4" />
                <span>(31) 3649-7064</span>
              </a>
              <Link 
                href="/login" 
                className="group inline-flex items-center space-x-2 px-4 py-2 bg-white text-primary border border-white rounded-md hover:bg-white/90 hover:shadow-md transition-all duration-300 font-medium"
              >
                <LogIn className="w-4 h-4 group-hover:rotate-12 transition-transform duration-300"/>
                <span>Entrar</span>
              </Link>
            </div>

            {/* Mobile menu button */}
            <button
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              className="md:hidden p-2"
            >
              {isMobileMenuOpen ? (
                <X className="w-6 h-6 text-white" />
              ) : (
                <Menu className="w-6 h-6 text-white" />
              )}
            </button>
          </div>

          {/* Mobile menu */}
          {isMobileMenuOpen && (
            <div className="md:hidden border-t border-white/20 py-4">
              <nav className="flex flex-col space-y-4">
                {/* Phone number for mobile */}
                <a href="tel:+5531364970064" className="flex items-center space-x-2 text-white hover:text-white/80 transition-colors px-4 py-2">
                  <Phone className="w-4 h-4" />
                  <span>(31) 3649-7064</span>
                </a>
                <div className="pt-4 border-t border-white/20 space-y-2">
                  <Link 
                    href="/login" 
                    className="group w-full inline-flex items-center justify-center space-x-2 px-4 py-3 bg-white text-primary border border-white rounded-md hover:bg-white/90 hover:shadow-md transition-all duration-300 font-medium"
                  >
                    <LogIn className="w-4 h-4 group-hover:rotate-12 transition-transform duration-300" />
                    <span>Entrar</span>
                  </Link>
                </div>
              </nav>
            </div>
          )}
        </div>
      </header>

      {/* Hero Section*/}
      <section className="bg-gradient-to-br from-primary-bg via-white to-primary-bg py-9">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              <div className="space-y-4">
                <h1 className="text-4xl lg:text-6xl font-bold text-gray-700 leading-tight">
                  Hortifruti
                  <span className="text-primary block">Santa Luzia</span>
                </h1>
                <p className="text-xl text-neutral-600 leading-relaxed">
                  Sistema completo de gestão para hortifrutis com controle de estoque, 
                  gestão financeira, cálculo de frete e muito mais.
                </p>
              </div>

              <div className="flex items-center space-x-8 text-sm text-neutral-600">
                <div className="flex items-center">
                  <CheckCircle className="w-5 h-5 text-primary mr-2" />
                  <span>Fácil de usar</span>
                </div>
                <div className="flex items-center">
                  <CheckCircle className="w-5 h-5 text-primary mr-2" />
                  <span>Suporte 24/7</span>
                </div>
                <div className="flex items-center">
                  <CheckCircle className="w-5 h-5 text-primary mr-2" />
                  <span>Seguro</span>
                </div>
              </div>
            </div>

            <div className="relative">
              <div className="relative z-10 animate-float">
                <div className="relative group">
                  <img 
                    src="/image.png" 
                    alt="Dashboard Hortifruti SL" 
                    className="rounded-2xl shadow-2xl transition-transform duration-700 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 rounded-2xl bg-gradient-to-tr from-transparent via-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
                  <div className="absolute -top-2 -right-2 w-4 h-4 bg-primary rounded-full animate-ping"></div>
                </div>
              </div>
              <div className="absolute -inset-4 bg-gradient-to-r from-primary/30 to-green-400/30 rounded-2xl blur-2xl opacity-40 animate-pulse"></div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Carousel */}
      <section id="features" className="py-20 bg-neutral-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl lg:text-5xl font-bold text-gray-700 mb-4">
              Funcionalidades Completas
            </h2>
            <p className="text-xl text-neutral-600 max-w-3xl mx-auto">
              Descubra todas as ferramentas que vão transformar a gestão do seu hortifruti
            </p>
          </div>

          <FeaturesCarousel features={features} />
        </div>
      </section>

      {/* Stats Section */}
      <StatsSection />

      {/* Footer */}
  <footer id="contact" className="bg-neutral-900 text-white py-1">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col items-center gap-2">
          <a href="tel:+5531364970064" className="flex items-center space-x-2 text-white hover:text-white/80 transition-colors text-sm mt-2">
            <Phone className="w-4 h-4" />
            <span>(31) 3649-7064</span>
          </a>
          <div className="border-t border-neutral-800 pt-2 mt-2 w-full text-center text-neutral-400 text-xs">
            <p>&copy; 2025 Hortifruti SL. Todos os direitos reservados.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}