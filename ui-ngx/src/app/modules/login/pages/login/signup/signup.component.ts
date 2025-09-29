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
import { FormGroup, UntypedFormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AppState } from '@app/core/core.state';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { AuthService } from '@app/core/public-api';
import { PageComponent } from '@app/shared/public-api';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent extends PageComponent implements OnInit {

  logo = 'https://matdash-angular-main.netlify.app/assets/images/logos/logo.svg';
  aText = [this.translate.instant("signup.welcome-message")];
  iSpeed = 100;
  iIndex = 0;
  iArrLength = this.aText[0].length;
  iScrollAt = 20;
  start = false;
  iTextPos = 0;
  sContents = '';
  iRow: number;
  isSubmitting = false;

  signUpFormGroup = this.fb.group({
    firstname: ['', Validators.required],
    lastname: ['', Validators.required],
    email: ['', [Validators.email, Validators.required]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(6)]]
  });


  constructor(protected store: Store<AppState>,
    private authService: AuthService,
    public fb: UntypedFormBuilder,
    private router: Router,
    private translate: TranslateService) {
    super(store);
  }
  ngOnInit(): void {
  }
  onSignUp() {
    if (this.signUpFormGroup.valid) {
      const password = this.signUpFormGroup.get('password')?.value;
      const confirmPassword = this.signUpFormGroup.get('confirmPassword')?.value;
      if (password === confirmPassword) {
        this.isSubmitting = true;
        const signupData = this.signUpFormGroup.value;
        this.authService.signUp(signupData).subscribe(
          response => {
            this.isSubmitting = false;
            this.store.dispatch(new ActionNotificationShow({
              message: this.translate.instant('signup.success-message'),
              type: 'success'
            }));
            this.router.navigate(['/signup/emailVerification'], { queryParams: { email: response.email } });
          },
          error => {
            this.isSubmitting = false;
            let errorMessage = this.translate.instant('signup.error-message');
            if (error.error.status === 400) {
              errorMessage = this.translate.instant('signup.invalid-data-error');
            } else if (error.error.status === 409) {
              errorMessage = this.translate.instant('signup.email-taken-error');
            }else if(error.error.status === 500 ){
              if(error.error.errorCode === 3 ){
                this.router.navigate(['/signup/emailVerification'], { queryParams: { email: this.signUpFormGroup.get('email')?.value, errorCode: error.error.errorCode } });              }

              errorMessage = this.translate.instant('signup.email-error');
            }
            this.store.dispatch(new ActionNotificationShow({
              message: errorMessage,
              type: 'error'
            }));
          }
        );
      } else {
        console.error('Passwords do not match');
        this.signUpFormGroup.get('confirmPassword')?.setErrors({ mismatch: true });
        this.store.dispatch(new ActionNotificationShow({
          message: this.translate.instant('signup.password-mismatch-message'),
          type: 'error'
        }));
      }
    } else {
      console.error('Form is invalid');
      this.store.dispatch(new ActionNotificationShow({
        message: this.translate.instant('signup.form-invalid-message'),
        type: 'error'
      }));
    }
  }

  typewriter() {
    this.sContents = ' ';
    this.iRow = Math.max(0, this.iIndex - this.iScrollAt);
    const destination = document.getElementById('typedtext');

    while (this.iRow < this.iIndex) {
      this.sContents += this.aText[this.iRow++] + '<br />';
    }

    if (destination) {
      destination.innerHTML = this.sContents + this.aText[this.iIndex].substring(0, this.iTextPos) + '|';
    }

    if (this.iTextPos++ === this.iArrLength) {
      this.iTextPos = 0;
      this.iIndex++;
      if (this.iIndex !== this.aText.length) {
        this.iArrLength = this.aText[this.iIndex].length;
        setTimeout(() => this.typewriter(), 500);
      }
      this.start = true;
    } else {
      setTimeout(() => this.typewriter(), this.iSpeed);
    }
  }

}
