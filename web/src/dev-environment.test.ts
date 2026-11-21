import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';

type PublishedPort = { host_ip?: string; published: string; target: number };
type ComposeService = {
  ports?: PublishedPort[];
  environment?: Record<string, string>;
  depends_on?: Record<string, { condition: string }>;
  restart?: string;
  healthcheck?: { test: string[] };
};
type DevEnvironment = { services: Record<string, ComposeService> };

const repositoryRoot = fileURLToPath(new URL('../..', import.meta.url));

function devEnvironment(): DevEnvironment {
  const resolved = execFileSync('docker', ['compose', 'config', '--format', 'json'], {
    cwd: repositoryRoot,
    encoding: 'utf8',
  });
  return JSON.parse(resolved);
}

function service(name: string): ComposeService {
  const found = devEnvironment().services[name];
  if (!found) throw new Error(`the dev environment has no ${name} service`);
  return found;
}

describe('the dev environment', () => {
  it('keeps the worksheet store on loopback port 5446 for local runs and the e2e server', () => {
    expect(service('postgres').ports).toEqual([
      expect.objectContaining({ host_ip: '127.0.0.1', published: '5446', target: 5432 }),
    ]);
  });

  it('answers on the address and port the dev probe reads', () => {
    expect(service('api').ports).toEqual([
      expect.objectContaining({ host_ip: '10.13.13.4', published: '7070', target: 7070 }),
    ]);
  });

  it('listens inside the container on the port it publishes', () => {
    expect(service('api').environment?.PORT).toBe('7070');
  });

  it('opens worksheets in the store it starts beside', () => {
    expect(service('api').environment?.DATABASE_URL).toBe(
      'postgresql://worksheets:worksheets@postgres:5432/worksheets',
    );
  });

  it('starts only after the worksheet store reports healthy', () => {
    expect(service('api').depends_on?.postgres?.condition).toBe('service_healthy');
  });

  it('comes back after the process or the daemon restarts', () => {
    expect(service('api').restart).toBe('unless-stopped');
  });

  it('reports healthy only when its home page names this project', () => {
    const probe = service('api').healthcheck?.test.join(' ') ?? '';
    expect(probe).toContain('http://127.0.0.1:7070/');
    expect(probe).toContain('Currency Converter');
  });
});
