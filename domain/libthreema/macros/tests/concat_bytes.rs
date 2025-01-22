#![allow(
    unused_crate_dependencies,
    clippy::min_ident_chars,
    clippy::tests_outside_test_module
)]

use libthreema_macros::concat_fixed_bytes;

#[test]
fn test_correct() {
    let a = [1_u8; 4];
    let b = [2_u8; 3];
    let c = [3_u8; 3];

    {
        #[allow(clippy::empty_structs_with_brackets)]
        let result: [u8; 0] = concat_fixed_bytes!();
        assert_eq!(result, [] as [u8; 0]);
    }

    {
        let result: [u8; 4] = concat_fixed_bytes!(a);
        assert_eq!(result, [1, 1, 1, 1]);
    }

    {
        let result: [u8; 7] = concat_fixed_bytes!(a, b);
        assert_eq!(result, [1, 1, 1, 1, 2, 2, 2]);
    }

    {
        let result: [u8; 10] = concat_fixed_bytes!(a, b, c);
        assert_eq!(result, [1, 1, 1, 1, 2, 2, 2, 3, 3, 3]);
    }
}

#[test]
fn compile_failures() {
    trybuild::TestCases::new().compile_fail("tests/fail/*.rs");
}
