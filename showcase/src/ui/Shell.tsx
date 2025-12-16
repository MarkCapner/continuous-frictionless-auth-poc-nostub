import React from "react";
import { NavLink } from "react-router-dom";

export type NavItem = {
  key: string;
  label: string;
  to: string;
  section?: string;
};

export function Shell(props: {
  title: string;
  subtitle?: string;
  items: NavItem[];
  topRight?: React.ReactNode;
  children: React.ReactNode;
}) {
  const { title, subtitle, items, topRight, children } = props;

  let lastSection: string | undefined;

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brandTitle">Continuous Frictionless Auth</div>
          <div className="brandSub">Showcase · Analyst · Admin / ML Ops</div>
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
                <NavLink
                  to={it.to}
                  className={({ isActive }) => `navBtn ${isActive ? "navBtnActive" : ""}`}
                  end
                >
                  <span>{it.label}</span>
                </NavLink>
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
