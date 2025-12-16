import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Shell, type NavItem } from "./ui/Shell";
import { ShowcasePage } from "./pages/showcase/ShowcasePage";
import { AnalystDashboardPage } from "./pages/analyst/AnalystDashboardPage";
import { AdminTlsPage } from "./pages/admin/AdminTlsPage";
import { AdminBehaviorPage } from "./pages/admin/AdminBehaviorPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminAnalyticsPage } from "./pages/admin/AdminAnalyticsPage";
import { AdminPolicyPage } from "./pages/admin/AdminPolicyPage";
import { AdminMlOpsPage } from "./pages/admin/AdminMlOpsPage";
import { GlobalSessionSelector } from "./ui/GlobalSessionSelector";
import { StickyRiskHeader } from "./ui/StickyRiskHeader";

type Header = { title: string; subtitle?: string };

function useHeader(): Header {
  const { pathname } = useLocation();

  if (pathname.startsWith("/admin/tls")) {
    return { title: "Admin / ML Ops · TLS", subtitle: "Inspect fingerprints, families, and clustering metadata." };
  }
  if (pathname.startsWith("/admin/behavior")) {
    return { title: "Admin / ML Ops · Behaviour", subtitle: "Per-user behavioural baselines and z-scores." };
  }
  if (pathname.startsWith("/admin/users")) {
    return { title: "Admin / ML Ops · Users", subtitle: "User and device summaries." };
  }
  if (pathname.startsWith("/admin/analytics")) {
    return { title: "Admin / ML Ops · Analytics", subtitle: "Session stats, risk breakdown and trends." };
  }
  if (pathname.startsWith("/admin/policy")) {
    return { title: "Admin / ML Ops · Policy", subtitle: "Create and manage policy rules (scope, conditions, actions)." };
  }
  if (pathname.startsWith("/admin/ml")) {
    return { title: "Admin / ML Ops · Model", subtitle: "Model status and re-training controls." };
  }
  if (pathname.startsWith("/analyst")) {
    return { title: "Analyst · Dashboard", subtitle: "Explainability views across sessions, devices and risk." };
  }

  return {
    title: "Showcase",
    subtitle:
      "Device profile, TLS fingerprinting, behavioural signals, and risk decisions. No cookies or local storage; everything is computed in-memory."
  };
}

export default function App() {
  const header = useHeader();

  const navItems: NavItem[] = [
    { key: "showcase", label: "Showcase", to: "/showcase", section: "Showcase" },
    { key: "analyst-dashboard", label: "Dashboard", to: "/analyst/dashboard", section: "Analyst" },
    { key: "admin-tls", label: "TLS fingerprints", to: "/admin/tls", section: "Admin / ML Ops" },
    { key: "admin-behavior", label: "Behaviour baselines", to: "/admin/behavior", section: "Admin / ML Ops" },
    { key: "admin-users", label: "Users", to: "/admin/users", section: "Admin / ML Ops" },
    { key: "admin-analytics", label: "Analytics", to: "/admin/analytics", section: "Admin / ML Ops" },
    { key: "admin-policy", label: "Policy", to: "/admin/policy", section: "Admin / ML Ops" },
    { key: "admin-ml", label: "ML model", to: "/admin/ml", section: "Admin / ML Ops" }
  ];

  return (
    <Shell
      title={`Continuous Frictionless Auth · ${header.title}`}
      subtitle={header.subtitle}
      items={navItems}
      topRight={<GlobalSessionSelector />}
      stickyHeader={<StickyRiskHeader />}
    >
      <Routes>
        {/* Tier 1: Showcase (default) */}
        <Route path="/" element={<Navigate to="/showcase" replace />} />
        <Route path="/showcase" element={<ShowcasePage />} />

        {/* Tier 2: Analyst */}
        <Route path="/analyst/dashboard" element={<AnalystDashboardPage />} />

        {/* Tier 3: Admin / ML Ops */}
        <Route path="/admin/tls" element={<AdminTlsPage />} />
        <Route path="/admin/behavior" element={<AdminBehaviorPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
        <Route path="/admin/policy" element={<AdminPolicyPage />} />
        <Route path="/admin/ml" element={<AdminMlOpsPage />} />

        {/* Backwards-compatible aliases from the old view keys */}
        <Route path="/showcase-dashboard" element={<Navigate to="/analyst/dashboard" replace />} />
        <Route path="/admin-tls" element={<Navigate to="/admin/tls" replace />} />
        <Route path="/admin-behavior" element={<Navigate to="/admin/behavior" replace />} />
        <Route path="/admin-users" element={<Navigate to="/admin/users" replace />} />
        <Route path="/admin-analytics" element={<Navigate to="/admin/analytics" replace />} />
        <Route path="/admin-policy" element={<Navigate to="/admin/policy" replace />} />
        <Route path="/admin-ml" element={<Navigate to="/admin/ml" replace />} />

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/showcase" replace />} />
      </Routes>
    </Shell>
  );
}
