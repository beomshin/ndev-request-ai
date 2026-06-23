import { createContext, useContext, useReducer, type ReactNode } from "react";
import {
  TOTAL_SLIDES,
  type AdditionalCheckItem,
  type AttachmentPlaceholder,
  type WizardAction,
  type WizardData,
  type WizardState,
} from "./types";

// 초기 상태 — createdAt만 미리 박아둔다 (S3 자동 표시용)
const initialState: WizardState = {
  currentSlide: 1,
  direction: 1,
  data: {
    createdAt: new Date().toISOString().slice(0, 10),
    additionalCheckItems: [],
    attachments: [],
    s6: {},
  },
};

function reducer(state: WizardState, action: WizardAction): WizardState {
  switch (action.type) {
    case "GOTO": {
      const next = clamp(action.slide, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: next >= state.currentSlide ? 1 : -1 };
    }
    case "NEXT": {
      const next = clamp(state.currentSlide + 1, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: 1 };
    }
    case "PREV": {
      const next = clamp(state.currentSlide - 1, 1, TOTAL_SLIDES);
      return { ...state, currentSlide: next, direction: -1 };
    }
    case "PATCH": {
      return { ...state, data: mergeData(state.data, action.patch) };
    }
    case "ADD_CHECK": {
      const exists = state.data.additionalCheckItems?.some(
        (it) => it.field === action.item.field,
      );
      if (exists) return state;
      return {
        ...state,
        data: {
          ...state.data,
          additionalCheckItems: [...(state.data.additionalCheckItems ?? []), action.item],
        },
      };
    }
    case "REMOVE_CHECK": {
      return {
        ...state,
        data: {
          ...state.data,
          additionalCheckItems: (state.data.additionalCheckItems ?? []).filter(
            (it) => it.field !== action.field,
          ),
        },
      };
    }
    case "ADD_ATTACHMENT": {
      return {
        ...state,
        data: {
          ...state.data,
          attachments: [...(state.data.attachments ?? []), action.file],
        },
      };
    }
    case "REMOVE_ATTACHMENT": {
      return {
        ...state,
        data: {
          ...state.data,
          attachments: (state.data.attachments ?? []).filter((f) => f.name !== action.name),
        },
      };
    }
    default:
      return state;
  }
}

// data 패치는 얕은 머지 + s6는 한 단계 더 깊게 머지 (cardOptions 등이 통째로 덮이지 않도록)
function mergeData(prev: WizardData, patch: WizardData): WizardData {
  const merged: WizardData = { ...prev, ...patch };
  if (patch.s6) {
    merged.s6 = {
      ...(prev.s6 ?? {}),
      ...patch.s6,
      cardOptions: patch.s6.cardOptions
        ? { ...(prev.s6?.cardOptions ?? {}), ...patch.s6.cardOptions }
        : prev.s6?.cardOptions,
    };
  }
  return merged;
}

function clamp(n: number, min: number, max: number) {
  return Math.max(min, Math.min(max, n));
}

// ─────────────────────────────────────────────────────────────────────
// Context
// ─────────────────────────────────────────────────────────────────────

type WizardCtx = {
  state: WizardState;
  goto: (slide: number) => void;
  next: () => void;
  prev: () => void;
  patch: (patch: WizardData) => void;
  addCheck: (item: AdditionalCheckItem) => void;
  removeCheck: (field: string) => void;
  addAttachment: (file: AttachmentPlaceholder) => void;
  removeAttachment: (name: string) => void;
};

const WizardContext = createContext<WizardCtx | null>(null);

export function WizardProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const value: WizardCtx = {
    state,
    goto: (slide) => dispatch({ type: "GOTO", slide }),
    next: () => dispatch({ type: "NEXT" }),
    prev: () => dispatch({ type: "PREV" }),
    patch: (patch) => dispatch({ type: "PATCH", patch }),
    addCheck: (item) => dispatch({ type: "ADD_CHECK", item }),
    removeCheck: (field) => dispatch({ type: "REMOVE_CHECK", field }),
    addAttachment: (file) => dispatch({ type: "ADD_ATTACHMENT", file }),
    removeAttachment: (name) => dispatch({ type: "REMOVE_ATTACHMENT", name }),
  };

  return <WizardContext.Provider value={value}>{children}</WizardContext.Provider>;
}

export function useWizard() {
  const ctx = useContext(WizardContext);
  if (!ctx) throw new Error("useWizard()는 <WizardProvider> 안에서만 사용 가능");
  return ctx;
}
