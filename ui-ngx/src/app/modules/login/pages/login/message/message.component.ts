///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { AppState, AuthService } from '@app/core/public-api';
import { PageComponent } from '@app/shared/public-api';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-message',
  templateUrl: './message.component.html',
  styleUrls: ['./message.component.scss']
})
export class MessageComponent extends PageComponent implements OnInit {
  email = '';
  errorCode :number = 0;

  constructor(
    protected store: Store<AppState>,
    private route: ActivatedRoute,
    private authService: AuthService,
    private translate: TranslateService
  ) {
    super(store);
  }
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.email = params.email;
      this.errorCode = params.errorCode;
    });
    console.log(this.errorCode)
  }
  resendVerificationEmail() {
    if (!this.email || !this.isValidEmail(this.email)) {
      this.store.dispatch(new ActionNotificationShow({
        message: `${this.translate.instant('validation.invalid-address')}: ${this.email}`,
        type: 'error'
      }));
      return;
    }
    this.authService.resendVerificationEmail(this.email).subscribe(
      response => {
        console.log('Verification email resent successfully:', response);
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('verification.email-resend-success'),
          type: 'success'
        }));
      },
      error => {
        let errorMessage = this.translate.instant('verification.email-resend-error'); // Message par défaut
        if (error.status) {
          switch (error.message.charAt(0)) {
            case '1':
              errorMessage = this.translate.instant('verification.email-not-found');
              break;
            case '2':
              errorMessage = this.translate.instant('verification.credentials-missing');
              break;
            case '3':
              errorMessage = this.translate.instant('verification.account-already-active');
              break;
            case '4':
              errorMessage = this.translate.instant('verification.server-error');
              break;
            default:
              errorMessage = this.translate.instant('verification.general-error');
          }
        }
        this.store.dispatch(new ActionNotificationShow({
          message: errorMessage,
          type: 'error'
        }));
      }
    );
  }

  private isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }
}