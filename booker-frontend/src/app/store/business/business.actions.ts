import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { BusinessResponse } from '../../core/business/business.service';

export const BusinessActions = createActionGroup({
  source: 'Business',
  events: {
    'Load My Businesses':         emptyProps(),
    'Load My Businesses Success': props<{ businesses: BusinessResponse[] }>(),
    'Load My Businesses Failure': props<{ error: string }>(),

    'Load All Businesses':         emptyProps(),
    'Load All Businesses Success': props<{ businesses: BusinessResponse[] }>(),
    'Load All Businesses Failure': props<{ error: string }>(),

    'Switch Tab': props<{ tab: 'my' | 'all' }>(),

    'Load Pending Businesses':         emptyProps(),
    'Load Pending Businesses Success': props<{ businesses: BusinessResponse[] }>(),
    'Load Pending Businesses Failure': props<{ error: string }>(),

    'Change Business Status':         props<{ id: number; status: string; reason?: string }>(),
    'Change Business Status Success': props<{ id: number; status: string }>(),
    'Change Business Status Failure': props<{ error: string }>(),
  },
});
