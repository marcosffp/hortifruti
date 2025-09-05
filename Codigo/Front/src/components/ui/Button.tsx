import type React from "react";

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "outline" | "text";
  size?: "sm" | "md" | "lg";
  fullWidth?: boolean;
  icon?: React.ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  children,
  variant = "primary",
  size = "md",
  fullWidth = false,
  icon,
  className = "",
  type = "button",
  disabled = false,
  ...props
}) => {
  const getButtonClasses = () => {
    let classes = "";

    // Base button styling
    classes +=
      variant === "primary"
        ? " btn-primary"
        : variant === "outline"
          ? " btn-outline"
          : "";

    // Size variations
    if (size === "sm") {
      classes += " text-sm py-1 px-3";
    } else if (size === "lg") {
      classes += " text-lg py-3 px-5";
    }

    // Width
    if (fullWidth) {
      classes += " w-full";
    }

    // Disabled state
    if (disabled) {
      classes += " opacity-60 cursor-not-allowed";
    }

    return `inline-flex items-center justify-center rounded-md transition-colors ${classes} ${className}`.trim();
  };

  return (
    <button
      className={getButtonClasses()}
      type={type}
      disabled={disabled}
      {...props}
    >
      {icon && <span className="mr-2">{icon}</span>}
      {children}
    </button>
  );
};

export default Button;
