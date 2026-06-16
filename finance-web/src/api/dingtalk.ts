export interface DingTalkClientContext {
  channel: 'dingtalk' | 'web'
  corpId?: string
  authCode?: string
  userId?: string
  userName?: string
  device?: 'mobile' | 'desktop'
  userAgent?: string
}

interface DingTalkRuntime {
  runtime?: {
    permission?: {
      requestAuthCode?: (options: {
        corpId: string
        onSuccess: (result: { code?: string }) => void
        onFail: (error: unknown) => void
      }) => void
    }
  }
}

declare global {
  interface Window {
    dd?: DingTalkRuntime
  }
}

const DINGTALK_UA_PATTERN = /DingTalk|AliApp\(DingTalk/i

export async function resolveDingTalkContext(): Promise<DingTalkClientContext> {
  const params = new URLSearchParams(window.location.search)
  const userAgent = window.navigator.userAgent
  const inDingTalk = DINGTALK_UA_PATTERN.test(userAgent) || params.get('channel') === 'dingtalk'
  const corpId = params.get('corpId') || import.meta.env.VITE_DINGTALK_CORP_ID || undefined
  const urlAuthCode = params.get('authCode') || undefined
  const jsapiAuthCode = inDingTalk && corpId && !urlAuthCode ? await requestAuthCode(corpId) : undefined

  return {
    channel: inDingTalk ? 'dingtalk' : 'web',
    corpId,
    authCode: urlAuthCode || jsapiAuthCode,
    userId: params.get('userId') || localStorage.getItem('finance-agent-user-id') || undefined,
    userName: params.get('userName') || localStorage.getItem('finance-agent-user-name') || undefined,
    device: /Mobile|Android|iPhone|iPad/i.test(userAgent) ? 'mobile' : 'desktop',
    userAgent,
  }
}

async function requestAuthCode(corpId: string) {
  const requestAuthCode = window.dd?.runtime?.permission?.requestAuthCode
  if (!requestAuthCode) {
    return undefined
  }

  return new Promise<string | undefined>((resolve) => {
    requestAuthCode({
      corpId,
      onSuccess: (result) => resolve(result.code),
      onFail: () => resolve(undefined),
    })
  })
}
