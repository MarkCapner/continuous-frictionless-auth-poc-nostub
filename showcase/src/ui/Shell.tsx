import React from "react";

export type NavItem = {
  key: string;
  label: string;
  section?: string;
};

export function Shell(props: {
  title: string;
  subtitle?: string;
  activeKey: string;
  items: NavItem[];
  onNavigate: (key: string) => void;
  topRight?: React.ReactNode;
  children: React.ReactNode;
}) {
  const { title, subtitle, activeKey, items, onNavigate, topRight, children } = props;

  let lastSection: string | undefined;

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brandTitle">Continuous Frictionless Auth</div>
          <div className="brandSub">Showcase · Dashboard · Admin</div>
        </div>
        <nav className="nav" aria-label="Primary navigation">
          {items.map((it) => {
            const sectionEl =
              it.section && it.section !== lastSection ? (
                <div key={`sec-${it.section}`} className="navSectionLabel">
                  {it.section}
                </div>
              ) : null;
            lastSection = it.section ?? lastSection;
            return (
              <React.Fragment key={it.key}>
                {sectionEl}
                <button
                  type="button"
                  className={`navBtn ${activeKey === it.key ? "navBtnActive" : ""}`}
                  onClick={() => onNavigate(it.key)}
                >
                  <span>{it.label}</span>
                </button>
              </React.Fragment>
            );
          })}
        </nav>
      </aside>

      <div className="content">
        <div className="topbar">
          <div className="topbarTitle">
            <h1>{title}</h1>
            {subtitle ? <p>{subtitle}</p> : <p />}
          </div>
          <div>{topRight}</div>
        </div>
        <div className="page">
          <div className="container">{children}</div>
        </div>
      </div>
    </div>
  );
}
